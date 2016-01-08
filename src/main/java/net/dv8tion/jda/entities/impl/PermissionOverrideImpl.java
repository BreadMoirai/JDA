/**
 *    Copyright 2015-2016 Austin Keener & Michael Ritter
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
package net.dv8tion.jda.entities.impl;

import net.dv8tion.jda.JDA;
import net.dv8tion.jda.Permission;
import net.dv8tion.jda.entities.*;

import java.util.Collections;
import java.util.List;

public class PermissionOverrideImpl implements PermissionOverride
{
    private final User user;
    private final Role role;
    private final Channel channel;
    private int allow;
    private int deny;

    public PermissionOverrideImpl(Channel channel, User user, Role role)
    {
        this.channel = channel;
        this.user = user;
        this.role = role;
    }

    @Override
    public int getAllowedRaw()
    {
        return allow;
    }

    @Override
    public int getInheritRaw()
    {
        return ~(allow | deny);
    }

    @Override
    public int getDeniedRaw()
    {
        return deny;
    }

    @Override
    public List<Permission> getAllowed()
    {
        return Collections.unmodifiableList(Permission.getPermissions(allow));
    }

    @Override
    public List<Permission> getInherit()
    {
        return Collections.unmodifiableList(Permission.getPermissions(getInheritRaw()));
    }

    @Override
    public List<Permission> getDenied()
    {
        return Collections.unmodifiableList(Permission.getPermissions(deny));
    }

    @Override
    public JDA getJDA()
    {
        return channel.getJDA();
    }

    @Override
    public User getUser()
    {
        return null;
    }

    @Override
    public Role getRole()
    {
        return null;
    }

    @Override
    public Channel getChannel()
    {
        return channel;
    }

    @Override
    public Guild getGuild()
    {
        return channel.getGuild();
    }

    @Override
    public boolean isUserOverride()
    {
        return getUser() != null;
    }

    @Override
    public boolean isRoleOverride()
    {
        return getRole() != null;
    }

    @Override
    public boolean equals(Object o)
    {
        if (!(o instanceof PermissionOverrideImpl))
            return false;

        PermissionOverrideImpl oPermOver = (PermissionOverrideImpl) o;
        return this.allow == oPermOver.allow && this.deny == oPermOver.deny;
    }

    public PermissionOverrideImpl setAllow(int allow)
    {
        this.allow = allow;
        return this;
    }

    public PermissionOverrideImpl setDeny(int deny)
    {
        this.deny = deny;
        return this;
    }
}