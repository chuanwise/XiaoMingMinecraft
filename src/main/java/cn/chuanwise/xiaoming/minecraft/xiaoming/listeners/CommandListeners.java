package cn.chuanwise.xiaoming.minecraft.xiaoming.listeners;

import cn.chuanwise.mclib.bukkit.net.protocol.ExecuteResponse;
import cn.chuanwise.pattern.ParameterPattern;
import cn.chuanwise.util.StringUtil;
import cn.chuanwise.xiaoming.annotation.EventListener;
import cn.chuanwise.xiaoming.contact.message.Message;
import cn.chuanwise.xiaoming.event.MessageEvent;
import cn.chuanwise.xiaoming.event.SimpleListeners;
import cn.chuanwise.xiaoming.minecraft.xiaoming.XMMCXiaoMingPlugin;
import cn.chuanwise.xiaoming.minecraft.xiaoming.command.Command;
import cn.chuanwise.xiaoming.minecraft.xiaoming.configuration.CommandConfiguration;
import cn.chuanwise.xiaoming.minecraft.xiaoming.configuration.PlayerInfo;
import cn.chuanwise.xiaoming.minecraft.xiaoming.configuration.ServerInfo;
import cn.chuanwise.xiaoming.minecraft.xiaoming.net.OnlineClient;
import cn.chuanwise.xiaoming.user.GroupXiaoMingUser;
import cn.chuanwise.xiaoming.user.XiaoMingUser;

import java.util.*;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

public class CommandListeners extends SimpleListeners<XMMCXiaoMingPlugin> {
    @EventListener
    public void onCommand(MessageEvent event) throws InterruptedException, TimeoutException {
        final XiaoMingUser user = event.getUser();
        final Message message = event.getMessage();

        final CommandConfiguration commandConfiguration = plugin.getCommandConfiguration();

        final List<Command> commands = commandConfiguration.getCommands();
        for (Command command : commands) {
            if (handleCommand(command, user, message) && command.isNonNext()) {
                return;
            }
        }
    }

    private boolean handleCommand(Command command, XiaoMingUser user, Message message) throws InterruptedException, TimeoutException {
        final String serializedMessage = message.serialize();

        // match
        final Set<ParameterPattern> compiledFormats = command.getCompiledFormats();
        ParameterPattern parameterPattern = null;
        Map<String, String> formatEnv = null;
        for (ParameterPattern compiledFormat : compiledFormats) {
            final Optional<Map<String, String>> optionalEnv = compiledFormat.parse(serializedMessage);
            if (optionalEnv.isPresent()) {
                formatEnv = optionalEnv.get();
                parameterPattern = compiledFormat;
                break;
            }
        }

        // 如果没有匹配就失败
        if (Objects.isNull(parameterPattern)) {
            return false;
        }

        // 检查标记
        final String groupTag = command.getGroupTag();
        if (StringUtil.notEmpty(groupTag)) {
            if (!(user instanceof GroupXiaoMingUser)) {
                return false;
            }
            if (!user.getContact().hasTag(groupTag)) {
                return false;
            }
        }

        final String accountTag = command.getAccountTag();
        if (StringUtil.notEmpty(accountTag)) {
            if (!user.hasTag(accountTag)) {
                return false;
            }
        }

        // 检查权限
        for (String permission : command.getPermissions()) {
            if (!user.requirePermission(permission)) {
                return true;
            }
        }

        if (!plugin.getServer().isBound()) {
            user.sendError("服务器尚未启动，请联系管理员");
            return true;
        }

        // 检查绑定
        final PlayerInfo playerInfo = plugin.getPlayerConfiguration().getPlayerInfo(user.getCode()).orElse(null);
        if (command.isMustBind()) {
            if (Objects.isNull(playerInfo)) {
                user.sendError("你还没有绑定玩家名，不能执行该指令");
                return true;
            }
        }

        final Map<String, Object> genericEnv = new HashMap<>();
        genericEnv.put("command", command);

        final Object senderPlayerObject;
        if (Objects.nonNull(playerInfo)) {
            senderPlayerObject = playerInfo;
        } else {
            senderPlayerObject = user;
        }

        genericEnv.put("sender", senderPlayerObject);
        genericEnv.put("player", senderPlayerObject);

        genericEnv.putAll(formatEnv);

        // 格式化指令
        final List<String> commands = command.getServerCommands()
                .stream()
                .map(user::format)
                .collect(Collectors.toList());

        // 获取所有带该标记的服务器
        final String serverTag = command.getServerTag();
        final Set<ServerInfo> taggedServerInfo = plugin.getSessionConfiguration().searchServerByTag(serverTag);
        final List<OnlineClient> onlineClients = plugin.getServer().searchOnlineClientByTag(serverTag);

        if (onlineClients.size() != taggedServerInfo.size() && !command.isAllowUncompletedExecution()) {
            user.sendError("需要执行指令的服务器没有全部连接到小明");
            return true;
        }

        for (OnlineClient onlineClient : onlineClients) {
            genericEnv.put("server", onlineClient.getServerInfo());

            final List<String> finalCommands = commands.stream()
                    .map(x -> xiaoMingBot.getLanguageManager().formatAdditional(x, genericEnv::get))
                    .collect(Collectors.toList());

            // identify
            switch (command.getIdentify()) {
                case CONSOLE:
                    for (String finalCommand : finalCommands) {
                        final ExecuteResponse response = onlineClient.getRemoteContact().getConsole().execute(finalCommand);
                        if (response instanceof ExecuteResponse.Succeed) {

                        }
                    }
                    break;
                case ALL_ONLINE_PLAYER:
                    break;
                case SPECIAL_ONLINE_PLAYER:
                    break;
                case ALL_BOUND_ONLINE_PLAYER_NAME:
                    break;
                case FIRST_BOUND_ONLINE_PLAYER_NAME:
                    break;
                default:
            }
        }

        return false;
    }
}
