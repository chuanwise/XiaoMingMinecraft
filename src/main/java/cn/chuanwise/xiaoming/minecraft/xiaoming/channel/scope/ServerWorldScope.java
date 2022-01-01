package cn.chuanwise.xiaoming.minecraft.xiaoming.channel.scope;

import cn.chuanwise.xiaoming.minecraft.xiaoming.Plugin;
import lombok.Data;

import java.util.*;

@Data
public class ServerWorldScope extends Scope {
    Map<String, Set<String>> serverWorlds = new HashMap<>();

    @Override
    public void sendMessage(Plugin plugin, List<String> messages) {
        if (serverWorlds.isEmpty()) {
            plugin.getServer().getOnlineClients().forEach(x -> {
                final String name = x.getServerInfo().getName();
                final Set<String> worldNames = serverWorlds.get(name);

                if (worldNames.isEmpty()) {
                    return;
                }

                x.sendWorldMessage(worldNames, messages);
            });
        }
    }

    @Override
    public String getDescription(Plugin plugin) {
        return null;
    }
}