package cn.chuanwise.xiaoming.minecraft.bukkit.listeners;

import cn.chuanwise.common.util.Arguments;
import cn.chuanwise.common.util.Throwables;
import cn.chuanwise.common.util.Times;
import cn.chuanwise.xiaoming.minecraft.bukkit.XMMCBukkitPlugin;
import cn.chuanwise.xiaoming.minecraft.bukkit.configuration.WhitelistConfiguration;
import cn.chuanwise.xiaoming.minecraft.bukkit.net.Client;
import cn.chuanwise.xiaoming.minecraft.bukkit.net.XMMCClientContact;
import cn.chuanwise.xiaoming.minecraft.protocol.PlayerBindInfo;
import cn.chuanwise.xiaoming.minecraft.protocol.PlayerVerifyCodeInfo;
import org.bukkit.Server;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class WhitelistListener implements Listener {
    @EventHandler
    void onPlayerJoin(PlayerJoinEvent event) {
        XMMCBukkitPlugin.getInstance().scheduler().runAsyncTask(() -> onPlayerJoinSync(event));
    }

    void onPlayerJoinSync(PlayerJoinEvent event) {
        final XMMCBukkitPlugin plugin = XMMCBukkitPlugin.getInstance();
        final WhitelistConfiguration configuration = plugin.getWhitelistConfiguration();
        if (!configuration.isEnable()) {
            return;
        }

        final Player player = event.getPlayer();
        final Map<String, Object> environment = new HashMap<>();
        environment.put("player", player.getName());
        environment.put("uuid", player.getUniqueId());
        environment.put("world", player.getWorld().getName());

        // 检查玩家是否绑定
        final Client client = plugin.getClient();
        if (client.isConnected()) {
            environment.put("server", client.getName());

            final XMMCClientContact clientContact = client.getClientContact();
            try {
                final Optional<PlayerBindInfo> playerBindInfo = clientContact.requestBindInfo(player.getName());
                // 如果绑定了那就没事了
                if (playerBindInfo.isPresent()) {
                    return;
                }

                final Optional<PlayerVerifyCodeInfo> optionalPlayerVerifyCodeInfo = clientContact.requestAllocatePlayerVerifyCode(player.getName());
                if (optionalPlayerVerifyCodeInfo.isPresent()) {
                    final PlayerVerifyCodeInfo info = optionalPlayerVerifyCodeInfo.get();

                    environment.put("verifyCode", info.getVerifyCode());
                    environment.put("timeout", Times.toTimeLength(info.getTimeout()));

                    doOperation(configuration.getNonBind(), player, environment);
                } else {
                    doOperation(configuration.getError(), player, environment);
                }
            } catch (Exception exception) {
                plugin.communicator().consoleErrorString("请求玩家 " + player.getName() + " 相关信息时出现异常\n" + Throwables.toStackTraces(exception));
                doOperation(configuration.getError(), player, environment);
            }
        } else {
            doOperation(configuration.getBotOffline(), player, environment);
        }
    }

    @SuppressWarnings("all")
    private void doOperation(WhitelistConfiguration.Operation operation, Player player, Map<String, Object> environment) {
        final XMMCBukkitPlugin plugin = XMMCBukkitPlugin.getInstance();
        final Server server = plugin.getServer();

        final WhitelistConfiguration.Operation.Command command = operation.getCommand();
        if (command.isEnable()) {
            final ConsoleCommandSender consoleSender = server.getConsoleSender();
            final List<String> consoleCommands = command.getConsole();
            if (!consoleCommands.isEmpty()) {
                plugin.scheduler().runTask(() -> {
                    for (String cmd : consoleCommands) {
                        final String finalCommand = Arguments.format(cmd, 10, environment);
                        server.dispatchCommand(consoleSender, finalCommand);
                    }
                });
            }

            final List<String> playerCommands = command.getPlayer();
            if (!playerCommands.isEmpty()) {
                plugin.scheduler().runTask(() -> {
                    for (String cmd : playerCommands) {
                        final String finalCommand = Arguments.format(cmd, 10, environment);
                        server.dispatchCommand(consoleSender, finalCommand);
                    }
                });
            }
        }

        final WhitelistConfiguration.Operation.Message message = operation.getMessage();
        if (message.isEnable()) {
            for (String msg : message.getMessages()) {
                player.sendMessage(msg);
            }
        }

        final WhitelistConfiguration.Operation.Kick kick = operation.getKick();
        if (kick.isEnable()) {
            plugin.scheduler().runTask(() -> {
                player.kickPlayer(Arguments.format(kick.getMessage(), 10, environment));
            });
        }
    }
}
