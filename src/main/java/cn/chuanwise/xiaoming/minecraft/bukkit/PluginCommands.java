package cn.chuanwise.xiaoming.minecraft.bukkit;

import cn.chuanwise.mclib.bukkit.BukkitPluginObject;
import cn.chuanwise.mclib.bukkit.OnCommand;
import org.bukkit.command.CommandSender;

import java.io.IOException;

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
            client.connect().orElseThrow().addListener(x -> {
                if (x.isSuccess()) {
                    communicator().success(sender, "net.connect.succeed");
                } else {
                    communicator().success(sender, "net.connect.failed");
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
            client.disconnectManually().orElseThrow().addListener(x -> {
                if (x.isSuccess()) {
                    communicator().success(sender, "net.disconnect.succeed");
                } else {
                    communicator().success(sender, "net.disconnect.failed");
                }
            });
        }
    }

    @OnCommand(value = "reconnect", permission = "xmmc.admin.reconnect")
    void reconnect(CommandSender sender) throws InterruptedException {
        final Client client = plugin.getClient();
        if (client.isConnected()) {
            client.disconnectManually().orElseThrow().sync();
        }

        client.connect().orElseThrow().addListener(x -> {
            if (x.isSuccess()) {
                communicator().success(sender, "net.reconnect.succeed");
            } else {
                communicator().success(sender, "net.reconnect.failed");
            }
        });
    }

    @OnCommand(value = "debug", permission = "xmmc.debug")
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
}
