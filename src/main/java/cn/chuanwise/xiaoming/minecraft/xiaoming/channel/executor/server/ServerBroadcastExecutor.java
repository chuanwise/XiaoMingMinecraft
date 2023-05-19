package cn.chuanwise.xiaoming.minecraft.xiaoming.channel.executor.server;

import cn.chuanwise.common.util.Strings;
import cn.chuanwise.xiaoming.minecraft.xiaoming.XMMCXiaoMingPlugin;
import cn.chuanwise.xiaoming.minecraft.xiaoming.channel.executor.FormatExecutor;
import cn.chuanwise.xiaoming.minecraft.xiaoming.configuration.ServerInfo;
import cn.chuanwise.xiaoming.minecraft.xiaoming.net.OnlineClient;
import lombok.Data;

import java.util.Map;

@Data
public class ServerBroadcastExecutor
        extends ServerExecutor
        implements FormatExecutor {
    String format;

    @Override
    public void execute(Map<String, Object> environment) {
        final XMMCXiaoMingPlugin plugin = XMMCXiaoMingPlugin.getInstance();

        for (OnlineClient onlineClient : plugin.getServer().getOnlineClients()) {
            final ServerInfo serverInfo = onlineClient.getServerInfo();
            if (Strings.isEmpty(serverTag) || serverInfo.hasTag(serverTag)) {
                environment.put("server", serverInfo);
                final String finalMessage = plugin.getXiaoMingBot().getLanguageManager().formatAdditional(format, environment::get);
                onlineClient.getRemoteContact().broadcastMessage(finalMessage);
            }
        }
    }
}
