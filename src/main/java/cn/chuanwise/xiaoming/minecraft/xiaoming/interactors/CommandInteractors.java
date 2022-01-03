package cn.chuanwise.xiaoming.minecraft.xiaoming.interactors;

import cn.chuanwise.mclib.net.Player;
import cn.chuanwise.mclib.net.protocol.ExecuteResponse;
import cn.chuanwise.mclib.util.ColorUtil;
import cn.chuanwise.util.CollectionUtil;
import cn.chuanwise.xiaoming.annotation.Filter;
import cn.chuanwise.xiaoming.annotation.FilterParameter;
import cn.chuanwise.xiaoming.annotation.Required;
import cn.chuanwise.xiaoming.interactor.SimpleInteractors;
import cn.chuanwise.xiaoming.minecraft.xiaoming.Plugin;
import cn.chuanwise.xiaoming.minecraft.xiaoming.net.OnlineClient;
import cn.chuanwise.xiaoming.minecraft.xiaoming.util.Words;
import cn.chuanwise.xiaoming.user.XiaomingUser;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeoutException;

@SuppressWarnings("all")
public class CommandInteractors extends SimpleInteractors<Plugin> {
    @Filter(Words.CONSOLE + Words.EXECUTE + " {服务器} {r:指令}")
    @Required("xmmc.admin.execute.console")
    void consoleExecute(XiaomingUser user,
                        @FilterParameter("服务器") OnlineClient onlineClient,
                        @FilterParameter("指令") String command) throws InterruptedException, TimeoutException {
        final ExecuteResponse response = onlineClient.getRemoteContact().getConsole().execute(command);
        showExecuteResponse(user, response);
    }

    @Filter(Words.SERVER + Words.PLAYER + Words.EXECUTE + " {服务器} {玩家名} {r:指令}")
    @Required("xmmc.admin.execute.player")
    void playerExecute(XiaomingUser user,
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
        showExecuteResponse(user, response);
    }

    void showExecuteResponse(XiaomingUser user, ExecuteResponse response) {
        if (response instanceof ExecuteResponse.Succeed) {
            final ExecuteResponse.Succeed succeed = (ExecuteResponse.Succeed) response;
            final List<String> messages = succeed.getMessages();

            if (messages.isEmpty()) {
                user.sendMessage("指令已执行，无任何消息");
            } else {
                user.sendMessage(CollectionUtil.toString(messages, ColorUtil::clearColors));
            }
            return;
        }
        if (response instanceof ExecuteResponse.Error) {
            final ExecuteResponse.Error error = (ExecuteResponse.Error) response;
            user.sendError("指令执行失败：" + ColorUtil.clearColors(error.getMessage()));
            return;
        }
        if (response instanceof ExecuteResponse.Offline) {
            user.sendError("玩家不在线，指令执行失败");
            return;
        }

        user.sendError("错误的响应信息：" + response);
    }
}
