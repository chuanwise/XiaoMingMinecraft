package cn.chuanwise.xiaoming.minecraft.xiaoming.channel.scope;

import cn.chuanwise.util.CollectionUtil;
import cn.chuanwise.xiaoming.minecraft.xiaoming.Plugin;
import lombok.Data;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

@Data
public class ServerScope extends Scope {
    Set<String> serverNames = new HashSet<>();

    @Override
    public void sendMessage(Plugin plugin, String message) {
        if (serverNames.isEmpty()) {
            plugin.getServer().getOnlineClients().forEach(x -> {
                x.getRemoteContact().sendMessage(message);
            });
        } else {
            plugin.getServer().getOnlineClients().forEach(x -> {
                if (!serverNames.contains(x.getServerInfo().getName())) {
                    return;
                }
                x.getRemoteContact().sendMessage(message);
            });
        }
    }

    @Override
    public String getDescription(Plugin plugin) {
        if (serverNames.isEmpty()) {
            return "所有在线服务器";
        } else {
            return "服务器：" + CollectionUtil.toString(serverNames, "、");
        }
    }
}