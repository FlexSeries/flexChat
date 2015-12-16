/**
 * Copyright 2015 Stealth2800 <http://stealthyone.com/>
 * Copyright 2015 Contributors <https://github.com/FlexSeries>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package me.st28.flexseries.flexchat.backend.chatter;

import me.st28.flexseries.flexchat.FlexChat;
import me.st28.flexseries.flexchat.api.channel.Channel;
import me.st28.flexseries.flexchat.api.channel.ChannelInstance;
import me.st28.flexseries.flexchat.api.chatter.Chatter;
import me.st28.flexseries.flexchat.api.chatter.ChatterConsole;
import me.st28.flexseries.flexchat.api.chatter.ChatterManager;
import me.st28.flexseries.flexchat.api.chatter.ChatterPlayer;
import me.st28.flexseries.flexchat.backend.channel.ChannelManagerImpl;
import me.st28.flexseries.flexlib.player.PlayerData;
import me.st28.flexseries.flexlib.player.PlayerManager;
import me.st28.flexseries.flexlib.player.PlayerReference;
import me.st28.flexseries.flexlib.player.data.DataProviderDescriptor;
import me.st28.flexseries.flexlib.player.data.PlayerDataProvider;
import me.st28.flexseries.flexlib.player.data.PlayerLoader;
import me.st28.flexseries.flexlib.plugin.FlexPlugin;
import me.st28.flexseries.flexlib.plugin.module.FlexModule;
import me.st28.flexseries.flexlib.plugin.module.ModuleDescriptor;
import me.st28.flexseries.flexlib.plugin.module.ModuleReference;
import me.st28.flexseries.flexlib.storage.flatfile.YamlFileManager;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class ChatterManagerImpl extends FlexModule<FlexChat> implements ChatterManager, Listener, PlayerDataProvider {

    private final Map<String, Chatter> chatters = new HashMap<>();

    public ChatterManagerImpl(FlexChat plugin) {
        super(plugin, "chatters", "Manages chatter data", new ModuleDescriptor().setGlobal(true).setSmartLoad(false).addHardDependency(new ModuleReference("FlexChat", "channels")));
    }

    @Override
    protected void handleEnable() {
        chatters.put(ChatterConsole.NAME, new ChatterConsole());

        registerPlayerDataProvider(new DataProviderDescriptor().onlineOnly(true));
    }

    @Override
    protected void handleSave(boolean async) {
        for (Chatter chatter : chatters.values()) {
            saveChatter(chatter);
        }
    }

    private void saveChatter(Chatter chatter) {
        if (chatter instanceof ChatterPlayer) {
            chatter.save(FlexPlugin.getGlobalModule(PlayerManager.class).getPlayerData(((ChatterPlayer) chatter).getUuid()).getDirectSection(FlexChat.class));
            return;
        }

        YamlFileManager file = new YamlFileManager(getDataFolder() + File.separator + chatter.getIdentifier() + ".yml");

        chatter.save(file.getConfig());

        file.save();
    }

    private void loadPlayerChatter(UUID uuid, PlayerData data) {
        String identifier = uuid.toString();
        if (chatters.containsKey(identifier)) {
            return;
        }

        ChatterPlayer chatter = new ChatterPlayer(uuid);

        Channel defaultChannel = FlexPlugin.getGlobalModule(ChannelManagerImpl.class).getDefaultChannel();

        ConfigurationSection config = data.getDirectSection(FlexChat.class);
        if (defaultChannel != null) {
            Collection<ChannelInstance> instances = defaultChannel.getInstances(chatter);
            if (instances.size() == 1) {
                config.set("active.channel", defaultChannel.getName());
                config.set("instances." + defaultChannel.getName(), new ArrayList<String>());
            }
        }

        chatter.load(config);
        chatters.put(identifier, chatter);
    }

    public Collection<Chatter> getChatters() {
        return Collections.unmodifiableCollection(chatters.values());
    }

    @Override
    public Chatter getChatter(CommandSender sender) {
        if (sender instanceof Player) {
            return chatters.get(((Player) sender).getUniqueId().toString());
        }
        return chatters.get(ChatterConsole.NAME);
    }

    @Override
    public Chatter getChatter(String identifier) {
        return chatters.get(identifier);
    }

    @Override
    public void loadPlayer(PlayerLoader loader, PlayerData data, PlayerReference player) {
        loadPlayerChatter(player.getUuid(), data);
    }

    @Override
    public void savePlayer(PlayerLoader loader, PlayerData data, PlayerReference player) {
        Chatter chatter = chatters.get(player.getUuid().toString());
        if (chatter != null) {
            saveChatter(chatter);
        }
    }

    @Override
    public boolean unloadPlayer(PlayerLoader loader, PlayerData data, PlayerReference player, boolean force) {
        Chatter chatter = chatters.remove(player.getUuid().toString());
        if (chatter != null) {
            for (ChannelInstance instance : chatter.getInstances()) {
                instance.removeOfflineChatter(chatter);
            }
        }
        return true;
    }

}