package cn.chuanwise.xiaoming.minecraft.bukkit.command;

import cn.chuanwise.commandlib.annotation.Format;
import cn.chuanwise.commandlib.annotation.Refer;
import cn.chuanwise.commandlib.command.Command;
import cn.chuanwise.common.util.*;
import cn.chuanwise.mclib.bukkit.plugin.BukkitPluginObject;
import cn.chuanwise.pandalib.annotation.Required;
import cn.chuanwise.xiaoming.minecraft.bukkit.XMMCBukkitPlugin;
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

    @Format("xm connect")
    @Required("xm.admin.net.connect")
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

    @Format("xm disconnect")
    @Required("xm.admin.net.disconnect")
    void disconnect(CommandSender sender) {
        final Client client = plugin.getClient();
        if (!client.isConnected()) {
            communicator().warn(sender, "net.disconnect.unnecessary");
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

    @Format("xm debug")
    @Required("xm.admin.debug")
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

    @Format("xm bind [qq]")
    @Required("xm.user.bind")
    void bind(CommandSender sender, @Refer("qq") String qqString) throws InterruptedException, TimeoutException {
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

    @Format("xm unbind")
    @Required("xm.user.unbind")
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

    @Format("xm reload")
    @Format("xm reload config")
    @Required("xm.admin.config.reload")
    void reload(CommandSender sender) {
        try {
            plugin.reload();
            communicator().info(sender, "reload.succeed");
        } catch (Exception exception) {
            communicator().info(sender, "reload.failed", Throwables.toStackTraces(exception));
        }
    }

    @Format("xm config host [host]")
    @Required("xm.admin.config.host")
    void configHost(CommandSender sender, @Refer("host") String host) throws IOException {
        final ConnectionConfiguration configuration = plugin.getConnectionConfiguration();
        configuration.setHost(host);
        configuration.save();
        communicator().info(sender, "configuration.host.configured", host);
    }

    @Format("xm config port [port]")
    @Required("xm.admin.config.port")
    void configPort(CommandSender sender, @Refer("port") String portString) throws IOException {
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

    @Format("xm help")
    @Required("xm.user.help")
    void helpExecutable(CommandSender sender) {
        final List<Command> commands = plugin.getCommandLib()
                .commandTree()
                .getCommands()
                .stream()
                .filter(x -> {
                    final String permission = x.getProperty(cn.chuanwise.pandalib.command.Properties.PERMISSION).orElseThrow();
                    return Strings.isEmpty(permission) || sender.hasPermission(permission);
                })
                .sorted(Comparator.comparing(Command::getFormat))
                .collect(Collectors.toList());

        communicator().info(sender, "help.executable", CollectionUtil.toString(commands,
                c -> {
                    final String permission = c.getProperty(cn.chuanwise.pandalib.command.Properties.PERMISSION).orElseThrow();
                    final boolean hasPermission = Strings.isEmpty(permission) || sender.hasPermission(permission);
                    return "§8- " + (hasPermission ? "§b" : "§3")
                            + "/" + c.getFormat()
                            + (Strings.nonEmpty(permission) ?
                            " §8§l: "
                                    + (hasPermission ? "§f" : "§7")
                                    + permission
                            : "");
                }, "\n"));
    }

    @Format("xm help all")
    void helpAll(CommandSender sender) {
        final List<Command> commands = plugin.getCommandLib()
                .commandTree()
                .getCommands()
                .stream()
                .sorted(Comparator.comparing(Command::getFormat))
                .collect(Collectors.toList());

        communicator().info(sender, "help.all", CollectionUtil.toString(commands,
                c -> {
                    final String permission = c.getProperty(cn.chuanwise.pandalib.command.Properties.PERMISSION).orElseThrow();
                    final boolean hasPermission = Strings.isEmpty(permission) || sender.hasPermission(permission);
                    return "§8- " + (hasPermission ? "§b" : "§3")
                            + "/" + c.getFormat()
                            + (Strings.nonEmpty(permission) ?
                            " §8§l: "
                                    + (hasPermission ? "§f" : "§7")
                                    + permission
                            : "");
                }, "\n"));
    }
}
