package net.dv8tion.jda.bot.sharding;

import com.mashape.unirest.http.Unirest;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.security.auth.login.LoginException;
import net.dv8tion.jda.bot.entities.ApplicationInfo;
import net.dv8tion.jda.core.JDA;
import net.dv8tion.jda.core.JDABuilder;
import net.dv8tion.jda.core.OnlineStatus;
import net.dv8tion.jda.core.entities.Game;
import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.entities.User;
import net.dv8tion.jda.core.entities.impl.JDAImpl;
import net.dv8tion.jda.core.exceptions.RateLimitedException;
import net.dv8tion.jda.core.requests.RestAction;
import org.apache.http.util.Args;

public class ShardManager // TODO: think about what methods ShardManager should contain
{
    private Thread loginThread;

    private List<JDAImpl> shards;
    private int shardsTotal;

    private List<JDA> shardsUnmodifiable;

    private AtomicBoolean shutdown = new AtomicBoolean(false);

    ShardManager() {}

    public RestAction<ApplicationInfo> getApplicationInfo()
    {
        return this.getShard(0).asBot().getApplicationInfo();
    }

    public double getAveragePing()
    {
        return this.shards.stream().mapToLong(jda -> jda.getPing()).average().getAsDouble();
    }

    public List<Guild> getGuilds()
    {
        return this.getStream(jda -> jda.getGuildMap().valueCollection().stream()).distinct()
                .collect(Collectors.toList());
    }

    public JDA getShard(final int shardId)
    {
        Args.positive(shardId, "shardId");
        if (shardId >= this.shardsTotal)
            throw new IllegalArgumentException(shardId + " may not be higher than shardsTotal");

        return this.shards.get(shardId);
    }

    public List<JDA> getShards()
    {
        return this.shardsUnmodifiable;
    }

    public JDA.Status getStatus(final int shardId)
    {
        Args.positive(shardId, "shardId");
        if (shardId >= this.shardsTotal)
            throw new IllegalArgumentException(shardId + " may not be higher than shardsTotal");

        return this.shards.get(shardId).getStatus();
    }

    public List<JDA.Status> getStatuses()
    {
        return this.shards.stream().map(jda -> jda.getStatus()).collect(Collectors.toList());
    }

    private <T> Stream<T> getStream(final Function<? super JDAImpl, ? extends Stream<? extends T>> mapper)
    {
        return this.shards.stream().flatMap(mapper);
    }

    public List<User> getUsers() // TODO: think about how resource intensive this is on large bots (over 1-2M users)
    {
        return this.getStream(jda -> jda.getUserMap().valueCollection().stream()).distinct()
                .collect(Collectors.toList());
    }

    public void login(final JDABuilder builder, final int shardsTotal) throws LoginException, IllegalArgumentException
    {
        Args.notNull(builder, "builder");

        this.shards = new ArrayList<>(shardsTotal);
        this.shardsUnmodifiable = Collections.unmodifiableList(this.shards);

        this.shardsTotal = shardsTotal;

        final AtomicInteger shard = new AtomicInteger(0);

        // building the first one in the currrent thread ensures that LoginException and IllegalArgumentException can be thrown
        try
        {
            builder.useSharding(shard.get(), shardsTotal);
            this.shards.add(shard.get(), (JDAImpl) builder.buildAsync());

            shard.incrementAndGet();
        }
        catch (final RateLimitedException e)
        {
            // do not increment 'shard' and try the first one again after 5 seconds in the async thread
        }

        this.loginThread = new Thread(() ->
        {
            do
            {
                try
                {
                    TimeUnit.SECONDS.sleep(5);
                }
                catch (final InterruptedException ignored) {}

                if (this.shutdown.get())
                    return;

                try
                {
                    builder.useSharding(shard.get(), shardsTotal);
                    this.shards.add(shard.get(), (JDAImpl) builder.buildAsync());

                    shard.incrementAndGet();
                }
                catch (LoginException | IllegalArgumentException e)
                {
                    // TODO: this should never happen unless the token changes inbetween
                    e.printStackTrace();
                }
                catch (final RateLimitedException e)
                {
                    // do not increment 'shard' and try the current one again after 5 seconds
                }
            }
            while (shard.get() < shardsTotal && !this.shutdown.get());
        }, "ShardManager-Login-Thread");
        this.loginThread.start();
    }

    public void setGame(final Game game)
    {
        this.shards.forEach(jda -> jda.getPresence().setGame(game));
    }

    public void setIdle(final boolean idle)
    {
        this.shards.forEach(jda -> jda.getPresence().setIdle(idle));
    }

    public void setStatus(final OnlineStatus status)
    {
        this.shards.forEach(jda -> jda.getPresence().setStatus(status));
    }

    public void shutdown()
    {
        this.shutdown(true);
    }

    public void shutdown(final boolean free)
    {
        if (this.shutdown.getAndSet(true))
            return; // shutdown has already been requested

        if (this.loginThread != null)
            this.loginThread.interrupt();

        if (this.shards != null)
            for (int i = 0; i < this.shards.size(); i++) // TODO: decide weather this should be done in parallel
                this.shards.get(i).shutdown(false);

        if (free)
            try
            {
                Unirest.shutdown();
            }
            catch (final IOException ignored) {}
    }
}
