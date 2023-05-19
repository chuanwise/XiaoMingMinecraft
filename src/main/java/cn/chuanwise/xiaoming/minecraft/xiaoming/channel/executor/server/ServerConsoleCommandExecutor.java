package cn.chuanwise.xiaoming.minecraft.xiaoming.channel.executor.server;

import cn.chuanwise.common.util.Strings;
import cn.chuanwise.xiaoming.minecraft.xiaoming.XMMCXiaoMingPlugin;
import cn.chuanwise.xiaoming.minecraft.xiaoming.channel.executor.CommandExecutor;
import cn.chuanwise.xiaoming.minecraft.xiaoming.configuration.ServerInfo;
import cn.chuanwise.xiaoming.minecraft.xiaoming.net.OnlineClient;
import lombok.Data;

import java.util.Map;
import java.util.concurrent.TimeoutException;

@Data
public class ServerConsoleCommandExecutor
        extends ServerExecutor
        implements CommandExecutor {
    String command;

    @Override
    public void execute(Map<String, Object> environment) {
        final XMMCXiaoMingPlugin plugin = XMMCXiaoMingPlugin.getInstance();

        for (OnlineClient onlineClient : plugin.getServer().getOnlineClients()) {
            final ServerInfo serverInfo = onlineClient.getServerInfo();
            if (Strings.isEmpty(serverTag) || serverInfo.hasTag(serverTag)) {
                environment.put("server", serverInfo);
                final String finalCommand = plugin.getXiaoMingBot().getLanguageManager().formatAdditional(command, environment::get);
                environment.put("command", finalCommand);

                String response;
                try {
                    response = onlineClient.getRemoteContact().getConsole().execute(finalCommand).toString();
                } catch (InterruptedException exception) {
                    response = "执行指令被取消";
                } catch (TimeoutException exception) {
                    response = "执行指令超时";
                }

                environment.put("result", response);
            }
        }
    }
}
