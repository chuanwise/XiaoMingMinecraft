package cn.chuanwise.xiaoming.minecraft.xiaoming.channel.scope;

import cn.chuanwise.util.CollectionUtil;
import cn.chuanwise.xiaoming.minecraft.xiaoming.Plugin;
import cn.chuanwise.xiaoming.util.MiraiCodeUtil;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ServerScope extends Scope {
    String serverTag;

    @Override
    public void sendMessage(List<String> messages) {
        final Plugin plugin = Plugin.getInstance();

        plugin.getServer()
                .getOnlineClients()
                .forEach(x -> {
            if (!x.getServerInfo().hasTag(serverTag)) {
                return;
            }
            messages.forEach(y -> {
                final String translatedMessage = MiraiCodeUtil.contentToString(y);
                x.getRemoteContact().sendMessage(translatedMessage);
            });
        });
    }

    @Override
    public String getDescription() {
        return "带有标签 #" + serverTag + " 的服务器";
    }
}