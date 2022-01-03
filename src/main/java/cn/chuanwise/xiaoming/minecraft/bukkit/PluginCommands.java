package cn.chuanwise.xiaoming.minecraft.bukkit;

import cn.chuanwise.mclib.bukkit.BukkitPluginObject;
import cn.chuanwise.mclib.bukkit.OnCommand;
import cn.chuanwise.mclib.bukkit.Parameter;
import cn.chuanwise.util.*;
import cn.chuanwise.xiaoming.minecraft.bukkit.net.Client;
import cn.chuanwise.xiaoming.minecraft.bukkit.net.XMMCClientContact;
import cn.chuanwise.xiaoming.minecraft.protocol.PlayerBindResponse;
import cn.chuanwise.xiaoming.minecraft.protocol.PlayerBindResultInform;
import cn.chuanwise.xiaoming.minecraft.protocol.XMMCProtocol;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.io.IOException;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.concurrent.TimeoutException;

public class PluginCommands extends BukkitPluginObject<Plugin> {
    public PluginCommands(Plugin plugin) {
        super(plugin);
    }

    @OnCommand(value = "connect", permission = "xmmc.admin.connect")
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

    @OnCommand(value = "disconnect", permission = "xmmc.admin.disconnect")
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

    @OnCommand(value = "debug", permission = "xmmc.admin.debug")
    void debug(CommandSender sender) throws IOException {
        final Configuration configuration = plugin.getConfiguration();
        final boolean setTo = !configuration.isDebug();
        configuration.setDebug(setTo);
        communicator().setDebug(setTo);
        configuration.save();

        if (setTo) {
            communicator().info(sender, "debug.enabled");
        } else {
            communicator().info(sender, "debug.disabled");
        }
    }

    @OnCommand(value = "bind @qq", permission = "xmmc.user.bind")
    void bind(CommandSender sender, @Parameter("qq") String qqString) throws InterruptedException, TimeoutException {
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
            final String timeoutLength = TimeUtil.toTimeLength(timeout);
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

    @OnCommand(value = "unbind", permission = "xmmc.user.unbind")
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

    @OnCommand(value = "reload", permission = "xmmc.admin.reload")
    void reload(CommandSender sender) {
        try {
            plugin.reload();
            communicator().info(sender, "reload.succeed");
        } catch (IOException exception) {
            communicator().info(sender, "reload.failed", ThrowableUtil.toStackTraces(exception));
        }
    }

    @OnCommand(value = "test heartbeat", permission = "xmmc.admin.test")
    void testHeartbeat(CommandSender sender) {
        final Client client = plugin.getClient();
        if (!client.isConnected()) {
            communicator().warn(sender, "net.state.disconnected");
            return;
        }
        final XMMCClientContact clientContact = client.getClientContact();

        client.getHeartbeatHandler().sendHeartbeat();
        communicator().success(sender, "net.heartbeat");
    }

    @OnCommand(value = "config host @host", permission = "xmmc.admin.config.host")
    void configHost(CommandSender sender, @Parameter("host") String host) throws IOException {
        final Configuration configuration = plugin.getConfiguration();
        configuration.getConnection().setHost(host);
        configuration.save();
        communicator().info(sender, "configuration.host.configured", host);
    }

    @OnCommand(value = "config port @port", permission = "xmmc.admin.config.port")
    void configPort(CommandSender sender, @Parameter("port") String portString) throws IOException {
        final Configuration configuration = plugin.getConfiguration();

        final Optional<Integer> optionalPort = NumberUtil.parseIndex(portString);
        if (!optionalPort.isPresent()) {
            communicator().info(sender, "configuration.port.failed", portString);
            return;
        }
        final int port = optionalPort.get();

        configuration.getConnection().setPort(port);
        configuration.save();
        communicator().info(sender, "configuration.port.configured", portString);
    }
}
