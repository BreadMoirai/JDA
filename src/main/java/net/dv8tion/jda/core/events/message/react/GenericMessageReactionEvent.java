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

package net.dv8tion.jda.core.events.message.react;

import net.dv8tion.jda.core.JDA;
import net.dv8tion.jda.core.entities.Member;
import net.dv8tion.jda.core.entities.MessageChannel;
import net.dv8tion.jda.core.entities.MessageReaction;
import net.dv8tion.jda.core.entities.User;
import net.dv8tion.jda.core.events.message.GenericMessageEvent;

public class GenericMessageReactionEvent extends GenericMessageEvent
{

    protected User issuer;
    protected MessageReaction reaction;

    public GenericMessageReactionEvent(JDA api, long responseNumber, User user, MessageReaction reaction)
    {
        super(api, responseNumber, reaction.getMessageIdLong());
        this.issuer = user;
        this.reaction = reaction;
    }

    public MessageChannel getChannel()
    {
        return reaction.getChannel();
    }

    public User getUser()
    {
        return issuer;
    }

    public Member getMember()
    {
        return issuer instanceof Member ? (Member) issuer : null;
    }

    public MessageReaction getReaction()
    {
        return reaction;
    }
}
