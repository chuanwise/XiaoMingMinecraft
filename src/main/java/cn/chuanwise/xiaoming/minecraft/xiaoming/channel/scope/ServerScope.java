package cn.chuanwise.xiaoming.minecraft.xiaoming.channel.scope;

import cn.chuanwise.util.CollectionUtil;
import cn.chuanwise.xiaoming.minecraft.xiaoming.Plugin;
import lombok.Data;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

@Data
public class ServerScope extends Scope {
    Set<String> serverNames = new HashSet<>();

    @Override
    public void sendMessage(Plugin plugin, List<String> messages) {
        if (serverNames.isEmpty()) {
            plugin.getServer().getOnlineClients().forEach(x -> {
                messages.forEach(y -> x.getRemoteContact().sendMessage(y));
            });
        } else {
            plugin.getServer().getOnlineClients().forEach(x -> {
                if (!serverNames.contains(x.getServerInfo().getName())) {
                    return;
                }
                messages.forEach(y -> x.getRemoteContact().sendMessage(y));
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