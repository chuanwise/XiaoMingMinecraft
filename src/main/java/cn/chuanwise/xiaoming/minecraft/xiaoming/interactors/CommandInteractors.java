package cn.chuanwise.xiaoming.minecraft.xiaoming.interactors;

import cn.chuanwise.mclib.bukkit.net.Player;
import cn.chuanwise.mclib.bukkit.net.protocol.ExecuteResponse;
import cn.chuanwise.mclib.util.Colors;
import cn.chuanwise.common.util.CollectionUtil;
import cn.chuanwise.xiaoming.annotation.Filter;
import cn.chuanwise.xiaoming.annotation.FilterParameter;
import cn.chuanwise.xiaoming.annotation.Required;
import cn.chuanwise.xiaoming.interactor.SimpleInteractors;
import cn.chuanwise.xiaoming.minecraft.xiaoming.XMMCXiaoMingPlugin;
import cn.chuanwise.xiaoming.minecraft.xiaoming.net.OnlineClient;
import cn.chuanwise.xiaoming.minecraft.xiaoming.net.Server;
import cn.chuanwise.xiaoming.minecraft.xiaoming.util.Words;
import cn.chuanwise.xiaoming.user.XiaoMingUser;
import cn.chuanwise.xiaoming.util.MiraiCodes;

import java.util.*;
import java.util.concurrent.TimeoutException;

@SuppressWarnings("all")
public class CommandInteractors
        extends SimpleInteractors<XMMCXiaoMingPlugin> {

    @Filter(Words.SERVER + Words.CONSOLE + Words.EXECUTE + " {服务器} {r:指令}")
    @Required("xmmc.admin.execute.console")
    void consoleExecute(XiaoMingUser user,
                        @FilterParameter("服务器") OnlineClient onlineClient,
                        @FilterParameter("指令") String command) throws InterruptedException, TimeoutException {
        final ExecuteResponse response = onlineClient.getRemoteContact().getConsole().execute(MiraiCodes.contentToString(command));
        user.sendMessage(executeResponseDetail(response));
    }

    @Filter(Words.CONSOLE + Words.EXECUTE + " {r:指令}")
    @Required("xmmc.admin.execute.console")
    void consoleExecute(XiaoMingUser user,
                        @FilterParameter("指令") String command) throws InterruptedException, TimeoutException {
        final Server server = plugin.getServer();
        if (!server.isBound()) {
            user.sendError("服务器尚未启动！");
            return;
        }

        final List<OnlineClient> onlineClients = server.getOnlineClients();
        if (onlineClients.isEmpty()) {
            user.sendError("目前没有任何服务器连接到小明，无法执行指令");
            return;
        }
        if (onlineClients.size() == 1) {
            final OnlineClient onlineClient = onlineClients.get(0);
            final ExecuteResponse response = onlineClient.getRemoteContact().getConsole().execute(MiraiCodes.contentToString(command));

            if (plugin.getSessionConfiguration().getServers().size() == 1) {
                user.sendMessage(executeResponseDetail(response));
            } else {
                user.sendMessage("指令在当前在线的「" + onlineClient.getServerInfo().getName() + "」上的运行结果：\n" + executeResponseDetail(response));
            }
        } else {
            user.sendMessage("不只有一个服务器连接到小明，应该明确要执行的后台");
        }
    }

    @Filter(Words.ALL + Words.CONSOLE + Words.EXECUTE + " {r:指令}")
    @Required("xmmc.admin.execute.console")
    void allConsoleExecute(XiaoMingUser user,
                           @FilterParameter("指令") String command) throws InterruptedException, TimeoutException {
        final Server server = plugin.getServer();
        if (!server.isBound()) {
            user.sendError("服务器尚未启动 (；′⌒`)");
            return;
        }

        final List<OnlineClient> onlineClients = server.getOnlineClients();
        if (onlineClients.isEmpty()) {
            user.sendError("没有任何服务器连接到小明");
            return;
        }

        command = MiraiCodes.contentToString(command);
        final Map<String, ExecuteResponse> responses = new HashMap<>();
        for (OnlineClient onlineClient : onlineClients) {
            final ExecuteResponse response = onlineClient.getRemoteContact().getConsole().execute(command);
            responses.put(onlineClient.getServerInfo().getName(), response);
        }

        if (responses.size() == 1) {
            user.sendMessage(executeResponseDetail(responses.values().iterator().next()));
        } else {
            user.sendMessage("指令在各服务器执行的结果：\n" +
                    CollectionUtil.toIndexString(responses.entrySet(), x -> x.getKey() + "：" + executeResponseDetail(x.getValue())));
        }
    }

    @Filter(Words.SERVER + Words.PLAYER + Words.EXECUTE + " {服务器} {玩家名} {r:指令}")
    @Filter(Words.PLAYER + Words.EXECUTE + " {服务器} {玩家名} {r:指令}")
    @Required("xmmc.admin.execute.player")
    void playerExecute(XiaoMingUser user,
                       @FilterParameter("服务器") OnlineClient onlineClient,
                       @FilterParameter("玩家名") String playerName,
                       @FilterParameter("指令") String command) throws InterruptedException, TimeoutException {
        final Optional<Player> optionalPlayer = onlineClient.getRemoteContact().getPlayer(playerName);
        if (!optionalPlayer.isPresent()) {
            user.sendMessage("玩家不在线，无法令其执行指令");
            return;
        }
        final Player player = optionalPlayer.get();

        final ExecuteResponse response = player.execute(command);
        user.sendMessage(executeResponseDetail(response));
    }

    private String executeResponseDetail(ExecuteResponse response) {
        if (response instanceof ExecuteResponse.Succeed) {
            final ExecuteResponse.Succeed succeed = (ExecuteResponse.Succeed) response;
            final List<String> messages = succeed.getMessages();

            if (messages.isEmpty()) {
                return "指令已执行，无任何消息";
            } else {
                return CollectionUtil.toString(messages, Colors::clearColors, "\n");
            }
        }
        if (response instanceof ExecuteResponse.Error) {
            final ExecuteResponse.Error error = (ExecuteResponse.Error) response;
            return "执行失败（" + Colors.clearColors(error.getMessage()) + "）";
        }
        if (response instanceof ExecuteResponse.Offline) {
            return "玩家不在线，指令执行失败";
        }

        return "错误的响应信息（" + response + "）";
    }
}
