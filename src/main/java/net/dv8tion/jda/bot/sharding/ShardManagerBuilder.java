/*
 *     Copyright 2015-2017 Austin Keener & Michael Ritter
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package net.dv8tion.jda.bot.sharding;

import java.util.concurrent.atomic.AtomicInteger;
import javax.security.auth.login.LoginException;
import net.dv8tion.jda.core.AccountType;
import net.dv8tion.jda.core.JDABuilder;
import net.dv8tion.jda.core.OnlineStatus;
import net.dv8tion.jda.core.audio.factory.IAudioSendFactory;
import net.dv8tion.jda.core.entities.Game;
import net.dv8tion.jda.core.events.Event;
import net.dv8tion.jda.core.events.ReadyEvent;
import net.dv8tion.jda.core.exceptions.RateLimitedException;
import net.dv8tion.jda.core.hooks.EventListener;
import net.dv8tion.jda.core.hooks.IEventManager;
import net.dv8tion.jda.core.hooks.SubscribeEvent;
import org.apache.http.HttpHost;
import org.apache.http.util.Args;

/**
 * Used to create new {@link net.dv8tion.jda.core.JDA} instances. This is also useful for making sure all of
 * your {@link net.dv8tion.jda.core.hooks.EventListener EventListeners} are registered
 * before {@link net.dv8tion.jda.core.JDA} attempts to log in.
 *
 * <p>A single JDABuilder can be reused multiple times. Each call to
 * {@link net.dv8tion.jda.core.JDABuilder#buildAsync() buildAsync()} or
 * {@link net.dv8tion.jda.core.JDABuilder#buildBlocking() buildBlocking()}
 * creates a new {@link net.dv8tion.jda.core.JDA} instance using the same information.
 * This means that you can have listeners easily registered to multiple {@link net.dv8tion.jda.core.JDA} instances.
 */
public class ShardManagerBuilder
{
    final JDABuilder builder = new JDABuilder(AccountType.BOT);
    private int shardsTotal;
    private int lowerBound = -1;
    private int upperBound = -1;

    /**
     * TODO: completely change docs
     * Creates a completely empty JDABuilder.
     * <br>If you use this, you need to set the  token using
     * {@link net.dv8tion.jda.core.JDABuilder#setToken(String) setToken(String)}
     * before calling {@link net.dv8tion.jda.core.JDABuilder#buildAsync() buildAsync()}
     * or {@link net.dv8tion.jda.core.JDABuilder#buildBlocking() buildBlocking()}
     *
     * @param  accountType
     *         The {@link net.dv8tion.jda.core.AccountType AccountType}.
     */
    public ShardManagerBuilder(final int shardsTotal)
    {
        this.setShardTotal(shardsTotal);
    }

    public ShardManagerBuilder addEventListener(final Object... listeners)
    {
        this.builder.addEventListener(listeners);
        return this;
    }

    /**
     * Builds a new {@link net.dv8tion.jda.core.JDA} instance and uses the provided token to start the login process.
     * <br>The login process runs in a different thread, so while this will return immediately, {@link net.dv8tion.jda.core.JDA} has not
     * finished loading, thus many {@link net.dv8tion.jda.core.JDA} methods have the chance to return incorrect information.
     * <br>The main use of this method is to start the JDA connect process and do other things in parallel while startup is
     * being performed like database connection or local resource loading.
     *
     * <p>If you wish to be sure that the {@link net.dv8tion.jda.core.JDA} information is correct, please use
     * {@link net.dv8tion.jda.core.JDABuilder#buildBlocking() buildBlocking()} or register an
     * {@link net.dv8tion.jda.core.hooks.EventListener EventListener} to listen for the
     * {@link net.dv8tion.jda.core.events.ReadyEvent ReadyEvent} .
     *
     * @throws  LoginException
     *          If the provided token is invalid.
     * @throws  IllegalArgumentException
     *          If the provided token is empty or null.
     * @throws  RateLimitedException
     *          If we are being Rate limited.
     *
     * @return A {@link net.dv8tion.jda.core.JDA} instance that has started the login process. It is unknown as
     *         to whether or not loading has finished when this returns.
     */
    public ShardManager buildAsync() throws LoginException, IllegalArgumentException, RateLimitedException
    {
        final ShardManager manager = new ShardManager();

        manager.login(this.builder, this.shardsTotal, this.lowerBound, this.upperBound);

        return manager;
    }

    /**
     * Builds a new {@link net.dv8tion.jda.core.JDA} instance and uses the provided token to start the login process.
     * <br>This method will block until JDA has logged in and finished loading all resources. This is an alternative
     * to using {@link net.dv8tion.jda.core.events.ReadyEvent ReadyEvent}.
     *
     * @throws  LoginException
     *          If the provided token is invalid.
     * @throws  IllegalArgumentException
     *          If the provided token is empty or null.
     * @throws  InterruptedException
     *          If an interrupt request is received while waiting for {@link net.dv8tion.jda.core.JDA} to finish logging in.
     *          This would most likely be caused by a JVM shutdown request.
     * @throws  RateLimitedException
     *          If we are being Rate limited.
     *
     * @return A {@link net.dv8tion.jda.core.JDA} Object that is <b>guaranteed</b> to be logged in and finished loading.
     */
    public ShardManager buildBlocking()
            throws LoginException, IllegalArgumentException, InterruptedException, RateLimitedException
    {
        final ShardsReadyListener listener = new ShardsReadyListener(this.upperBound - this.lowerBound + 1);
        this.addEventListener(listener);
        final ShardManager manager = this.buildAsync();
        synchronized (listener)
        {
            while (!listener.allReady())
                listener.wait();
        }
        return manager;
    }

    public ShardManagerBuilder removeEventListener(final Object... listeners)
    {
        this.builder.removeEventListener(listeners);
        return this;
    }

    public ShardManagerBuilder setAudioEnabled(final boolean enabled)
    {
        this.builder.setAudioEnabled(enabled);
        return this;
    }

    public ShardManagerBuilder setAudioSendFactory(final IAudioSendFactory factory)
    {
        this.builder.setAudioSendFactory(factory);
        return this;
    }

    public ShardManagerBuilder setAutoReconnect(final boolean autoReconnect)
    {
        this.builder.setAutoReconnect(autoReconnect);
        return this;
    }

    public ShardManagerBuilder setBulkDeleteSplittingEnabled(final boolean enabled)
    {
        this.builder.setBulkDeleteSplittingEnabled(enabled);
        return this;
    }

    public ShardManagerBuilder setEnableShutdownHook(final boolean enable)
    {
        this.builder.setEnableShutdownHook(enable);
        return this;
    }

    public ShardManagerBuilder setEventManager(final IEventManager manager)
    {
        this.builder.setEventManager(manager);
        return this;
    }

    public ShardManagerBuilder setGame(final Game game)
    {
        this.builder.setGame(game);
        return this;
    }

    public ShardManagerBuilder setIdle(final boolean idle)
    {
        this.builder.setIdle(idle);
        return this;
    }

    public ShardManagerBuilder setProxy(final HttpHost proxy)
    {
        this.builder.setProxy(proxy);
        return this;
    }

    public ShardManagerBuilder setShardRange(final int lowerBound, final int upperBound)
    {
        Args.notNegative(lowerBound, "lowerBound");
        Args.check(upperBound < this.shardsTotal, "upperBound must be lower than shardsTotal");
        Args.check(lowerBound <= upperBound, "lowerBound must be lower than or equal to upperBound");

        this.lowerBound = lowerBound;
        this.upperBound = upperBound;
        return this;
    }

    /**
     * TODO: completely change docs
     *
     * This will enable sharding mode for JDA.
     * <br>In sharding mode, guilds are split up and assigned one of multiple shards (clients).
     * <br>The shardId that receives all stuff related to given bot is calculated as follows: shardId == (guildId {@literal >>} 22) % shardsTotal;
     * <br><b>PMs are only sent to shard 0.</b>
     *
     * <p>Please note, that a shard will not even know about guilds not assigned to.
     *
     * @param  shardId
     *         The id of this shard (starting at 0).
     * @param  shardsTotal
     *         The number of overall shards.
     *
     * @return Returns the {@link net.dv8tion.jda.core.JDABuilder JDABuilder} instance. Useful for chaining.
     *
     * @see    net.dv8tion.jda.core.JDA#getShardInfo() JDA.getShardInfo()
     */
    public ShardManagerBuilder setShardTotal(final int shardsTotal)
    {
        Args.positive(shardsTotal, "shardsTotal");
        this.shardsTotal = shardsTotal;
        if (this.lowerBound == -1 && this.upperBound == -1)
            this.setShardRange(0, shardsTotal - 1);

        return this;
    }

    public ShardManagerBuilder setStatus(final OnlineStatus status)
    {
        this.builder.setStatus(status);
        return this;
    }

    public ShardManagerBuilder setToken(final String token)
    {
        this.builder.setToken(token);
        return this;
    }

    public ShardManagerBuilder setWebSocketTimeout(final int websocketTimeout)
    {
        this.builder.setWebSocketTimeout(websocketTimeout);
        return this;
    }

    private static class ShardsReadyListener implements EventListener
    {
        private final AtomicInteger numReady = new AtomicInteger(0);
        private final int shardsTotal;

        public ShardsReadyListener(final int shardsTotal)
        {
            this.shardsTotal = shardsTotal;
        }

        public boolean allReady()
        {
            return this.numReady.get() == this.shardsTotal;
        }

        @Override
        @SubscribeEvent
        public void onEvent(final Event event)
        {
            if (event instanceof ReadyEvent)
            {
                event.getJDA().removeEventListener(this);
                if (this.numReady.incrementAndGet() == this.shardsTotal)
                    synchronized (this)
                    {
                        this.notifyAll();
                    }
            }
        }
    }
}
