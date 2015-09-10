/**
 * FlexChat - Licensed under the MIT License (MIT)
 *
 * Copyright (c) Stealth2800 <http://stealthyone.com/>
 * Copyright (c) contributors <https://github.com/FlexSeries>
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package me.st28.flexseries.flexchat.commands.channel;

import me.st28.flexseries.flexchat.FlexChat;
import me.st28.flexseries.flexchat.api.channel.Channel;
import me.st28.flexseries.flexchat.api.channel.ChannelInstance;
import me.st28.flexseries.flexchat.api.chatter.Chatter;
import me.st28.flexseries.flexchat.backend.chatter.ChatterManagerImpl;
import me.st28.flexseries.flexchat.commands.arguments.ChannelArgument;
import me.st28.flexseries.flexchat.commands.arguments.ChannelInstanceArgument;
import me.st28.flexseries.flexchat.permissions.PermissionNodes;
import me.st28.flexseries.flexlib.command.CommandContext;
import me.st28.flexseries.flexlib.command.CommandDescriptor;
import me.st28.flexseries.flexlib.command.CommandInterruptedException;
import me.st28.flexseries.flexlib.command.CommandInterruptedException.InterruptReason;
import me.st28.flexseries.flexlib.command.FlexCommand;
import me.st28.flexseries.flexlib.message.MessageManager;
import me.st28.flexseries.flexlib.message.ReplacementMap;
import me.st28.flexseries.flexlib.plugin.FlexPlugin;
import org.bukkit.command.CommandSender;

public final class CmdChannel extends FlexCommand<FlexChat> {

    public CmdChannel(FlexChat plugin) {
        super(
                plugin,
                new CommandDescriptor("channel").defaultCommand("list")
        );

        addArgument(new ChannelArgument("channel", true));
        addArgument(new ChannelInstanceArgument("instance", false, "channel"));

        //registerSubcommand(new SCmdChannelInfo(this));
        registerSubcommand(new SCmdChannelJoin(this));
        registerSubcommand(new SCmdChannelLeave(this));
        registerSubcommand(new SCmdChannelList(this));
        registerSubcommand(new SCmdChannelWho(this));
    }

    @Override
    public void handleExecute(CommandContext context) {
        // TODO: Make it so admins can join any channel instance.

        CommandSender sender = context.getSender();
        Chatter chatter = FlexPlugin.getGlobalModule(ChatterManagerImpl.class).getChatter(sender);

        Channel channel = context.getGlobalObject("channel", Channel.class);
        ChannelInstance instance = context.getGlobalObject("instance", ChannelInstance.class);

        if (!chatter.isInInstance(instance) && !chatter.hasPermission(PermissionNodes.buildVariableNode(PermissionNodes.JOIN, channel.getName()))) {
            throw new CommandInterruptedException(InterruptReason.COMMAND_SOFT_ERROR, MessageManager.getMessage(FlexChat.class, "errors.channel_no_permission", new ReplacementMap("{VERB}", "join").put("{CHANNEL}", channel.getName()).getMap()));
        }

        if (chatter.addInstance(instance)) {
            instance.sendMessage(MessageManager.getMessage(FlexChat.class, "alerts_channel.chatter_joined", new ReplacementMap("{CHATTER}", chatter.getName()).put("{COLOR}", channel.getColor().toString()).put("{CHANNEL}", channel.getName()).getMap()));
        }

        if (chatter.setActiveInstance(instance)) {
            throw new CommandInterruptedException(InterruptReason.COMMAND_END, MessageManager.getMessage(FlexChat.class, "notices.channel_active_set", new ReplacementMap("{COLOR}", channel.getColor().toString()).put("{CHANNEL}", channel.getName()).getMap()));
        } else {
            throw new CommandInterruptedException(InterruptReason.COMMAND_SOFT_ERROR, MessageManager.getMessage(FlexChat.class, "errors.channel_active_already_set", new ReplacementMap("{CHANNEL}", channel.getName()).getMap()));
        }
    }

}