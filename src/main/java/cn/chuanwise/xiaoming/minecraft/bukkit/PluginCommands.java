package cn.chuanwise.xiaoming.minecraft.bukkit;

import cn.chuanwise.commandlib.annotation.Description;
import cn.chuanwise.commandlib.annotation.Format;
import cn.chuanwise.commandlib.annotation.Permission;
import cn.chuanwise.commandlib.annotation.Reference;
import cn.chuanwise.commandlib.command.Command;
import cn.chuanwise.mclib.bukkit.plugin.BukkitPluginObject;
import cn.chuanwise.util.*;
import cn.chuanwise.util.Collections;
import cn.chuanwise.xiaoming.minecraft.bukkit.configuration.BaseConfiguration;
import cn.chuanwise.xiaoming.minecraft.bukkit.configuration.ConnectionConfiguration;
import cn.chuanwise.xiaoming.minecraft.bukkit.net.Client;
import cn.chuanwise.xiaoming.minecraft.bukkit.net.XMMCClientContact;
import cn.chuanwise.xiaoming.minecraft.protocol.PlayerBindResponse;
import cn.chuanwise.xiaoming.minecraft.protocol.PlayerBindResultInform;
import cn.chuanwise.xiaoming.minecraft.protocol.XMMCProtocol;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

public class PluginCommands
        extends BukkitPluginObject<XMMCBukkitPlugin> {
    public PluginCommands(XMMCBukkitPlugin plugin) {
        super(plugin);
    }

    @Description("尝试和小明建立连接")
    @Format("xm|xiaomingminecraft|xmmc connect")
    @Permission("xmmc.admin.connect")
    void connect(CommandSender sender) {
        final Client client = plugin.getClient();
        if (client.isConnected()) {
            communicator().warn(sender, "net.connect.multi");
        } else {
            client.connect().orElseThrow(NoSuchElementException::new).addListener(x -> {
                if (x.isSuccess()) {
                    communicator().success(sender, "net.connect.succeed");
                } else {
                    communicator().warn(sender, "net.connect.failed");
                }
            });
        }
    }

    @Description("断开和小明的连接")
    @Format("xm|xiaomingminecraft|xmmc disconnect")
    @Permission("xmmc.admin.disconnect")
    void disconnect(CommandSender sender) {
        final Client client = plugin.getClient();
        if (!client.isConnected()) {
            communicator().warn(sender, "net.disconnect.multi");
        } else {
            client.disconnectManually().orElseThrow(NoSuchElementException::new).addListener(x -> {
                if (x.isSuccess()) {
                    communicator().success(sender, "net.disconnect.succeed");
                } else {
                    communicator().success(sender, "net.disconnect.failed");
                }
            });
        }
    }

    @Description("开关调试模式")
    @Format("xm|xiaomingminecraft|xmmc debug")
    @Permission("xmmc.admin.debug")
    void debug(CommandSender sender) throws IOException {
        final BaseConfiguration baseConfiguration = plugin.getBaseConfiguration();
        final boolean setTo = !baseConfiguration.isDebug();
        baseConfiguration.setDebug(setTo);
        communicator().setDebug(setTo);
        baseConfiguration.save();

        if (setTo) {
            communicator().info(sender, "debug.enabled");
        } else {
            communicator().info(sender, "debug.disabled");
        }
    }

    @Description("绑定 QQ")
    @Format("xm|xiaomingminecraft|xmmc bind [qq]")
    @Permission("xmmc.user.bind")
    void bind(CommandSender sender,
              @Reference("qq") String qqString) throws InterruptedException, TimeoutException {
        final Optional<Long> optionalLong = NumberUtil.parseLong(qqString);
        if (!optionalLong.isPresent()) {
            communicator().errorString(sender, "qwq");
            return;
        }
        final long accountCode = optionalLong.get();

        if (!(sender instanceof Player)) {
            communicator().error(sender, "playerInfo.bind.operator");
            return;
        }

        final Client client = plugin.getClient();
        if (!client.isConnected()) {
            communicator().warn(sender, "net.state.disconnected");
            return;
        }

        final XMMCClientContact clientContact = client.getClientContact();
        final PlayerBindResponse response = clientContact.requestBind(sender.getName(), accountCode);

        if (response instanceof PlayerBindResponse.Wait) {
            final PlayerBindResponse.Wait wait = (PlayerBindResponse.Wait) response;
            final long timeout = wait.getTimeout();
            final String timeoutLength = Times.toTimeLength(timeout);
            communicator().info(sender, "playerInfo.bind.wait", timeoutLength);

            // 这里也开一个新线程等待一下
            plugin.scheduler().runAsyncTask(() -> {
                try {
                    final PlayerBindResultInform inform = client.getPacketHandler().nextData(XMMCProtocol.INFORM_PLAYER_BIND_RESULT, timeout);
                    switch (inform) {
                        case DENIED:
                            communicator().warn(sender, "playerInfo.bind.denied");
                            break;
                        case ACCEPTED:
                            communicator().success(sender, "playerInfo.bind.accepted");
                            break;
                        case TIMEOUT:
                            communicator().success(sender, "playerInfo.bind.timeout");
                            break;
                        case INTERRUPTED:
                            communicator().success(sender, "playerInfo.bind.interrupted");
                            break;
                        default:
                            throw new NoSuchElementException();
                    }
                } catch (InterruptedException exception) {
                    communicator().error(sender, "net.state.interrupted");
                } catch (TimeoutException exception) {
                    communicator().warn(sender, "net.state.timeout");
                }
            });
            return;
        }
        if (response instanceof PlayerBindResponse.Error) {
            final PlayerBindResponse.Error error = (PlayerBindResponse.Error) response;
            switch (error.getType()) {
                case OTHER:
                    communicator().warn(sender, "playerInfo.bind.other");
                    break;
                case FAILED:
                    communicator().warn(sender, "playerInfo.bind.failed");
                    break;
                case REPEAT:
                    communicator().warn(sender, "playerInfo.bind.repeat");
                    break;
                default:
                    throw new NoSuchElementException();
            }
        }
    }

    @Description("和自己已经绑定的 QQ 解绑")
    @Format("xm|xiaomingminecraft|xmmc unbind")
    @Permission("xmmc.user.unbind")
    void unbind(CommandSender sender) {
        final Client client = plugin.getClient();
        if (!client.isConnected()) {
            communicator().warn(sender, "net.state.disconnected");
            return;
        }
        final XMMCClientContact clientContact = client.getClientContact();

        if (!(sender instanceof Player)) {
            communicator().error(sender, "playerInfo.unbind.operator");
            return;
        }

        try {
            if (clientContact.requestUnbind(sender.getName())) {
                communicator().success(sender, "playerInfo.unbind.succeed");
            } else {
                communicator().warn(sender, "playerInfo.unbind.failed");
            }
        } catch (InterruptedException exception) {
            communicator().error(sender, "net.state.interrupted");
        } catch (TimeoutException exception) {
            communicator().error(sender, "net.state.timeout");
        }
    }

    @Description("重新载入插件数据")
    @Format("xm|xiaomingminecraft|xmmc reload")
    @Permission("xmmc.admin.config.reload")
    void reload(CommandSender sender) {
        try {
            plugin.reload();
            communicator().info(sender, "reload.succeed");
        } catch (IOException exception) {
            communicator().info(sender, "reload.failed", Throwables.toStackTraces(exception));
        }
    }

    @Description("修改小明主机地址")
    @Format("xm|xiaomingminecraft|xmmc config host [host]")
    @Permission("xmmc.admin.config.host")
    void configHost(CommandSender sender,
                    @Reference("host") String host) throws IOException {
        final ConnectionConfiguration configuration = plugin.getConnectionConfiguration();
        configuration.setHost(host);
        configuration.save();
        communicator().info(sender, "configuration.host.configured", host);
    }

    @Description("修改小明端口")
    @Format("xm|xiaomingminecraft|xmmc config port [port]")
    @Permission("xmmc.admin.config.port")
    void configPort(CommandSender sender,
                    @Reference("port") String portString) throws IOException {
        final ConnectionConfiguration configuration = plugin.getConnectionConfiguration();

        final Optional<Integer> optionalPort = NumberUtil.parseIndex(portString);
        if (!optionalPort.isPresent()) {
            communicator().info(sender, "configuration.port.failed", portString);
            return;
        }
        final int port = optionalPort.get();

        configuration.setPort(port);
        configuration.save();
        communicator().info(sender, "configuration.port.configured", portString);
    }

    @Description("查看自己可执行的指令列表")
    @Format("xm|xiaomingminecraft|xmmc help")
    @Permission("xmmc.user.help")
    void helpExecutable(CommandSender sender) {
        final List<Command> commands = plugin.getCommandLib()
                .getCommandManager()
                .getCommands()
                .stream()
                .filter(x -> {
                    final String permission = x.getPermission();
                    return Strings.isEmpty(permission) || sender.hasPermission(permission);
                })
                .sorted(Comparator.comparing(Command::getUsage))
                .collect(Collectors.toList());

        communicator().error(sender, "help.executable", CollectionUtil.toIndexString(commands,
                (i, c) -> "§3" + (i + 1) + "§7. ",
                c -> {
                    final boolean hasPermission = Strings.isEmpty(c.getPermission()) || sender.hasPermission(c.getPermission());
                    return (hasPermission ? "§b" : "§3")
                            + c.getUsage()
                            + (Strings.nonEmpty(c.getDescription()) ?
                            " §8§l: "
                                    + (hasPermission ? "§f" : "§7")
                                    + c.getDescription()
                            : "");
                }));
    }

    @Description("查看所有指令列表")
    @Format("xm|xiaomingminecraft|xmmc help all")
    @Permission("xmmc.user.help")
    void helpAll(CommandSender sender) {
        final List<Command> commands = plugin.getCommandLib()
                .getCommandManager()
                .getCommands()
                .stream()
                .sorted(Comparator.comparing(Command::getUsage))
                .collect(Collectors.toList());

        communicator().info(sender, "help.all", CollectionUtil.toIndexString(commands,
                (i, c) -> "§3" + (i + 1) + "§7. ",
                c -> {
                    final boolean hasPermission = Strings.isEmpty(c.getPermission()) || sender.hasPermission(c.getPermission());
                    return (hasPermission ? "§b" : "§3")
                            + c.getUsage()
                            + (Strings.nonEmpty(c.getDescription()) ?
                                    " §8§l: "
                                    + (hasPermission ? "§f" : "§7")
                                    + c.getDescription()
                            : "");
                }));
    }

//    @Format("xm|xiaomingminecraft|xmmc request [command~]")
//    @Permission("xmmc.admin.config.port")
//    void requestExecute(CommandSender sender,
//                        @Reference("command") String command) {
//
//    }
}
