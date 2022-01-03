package cn.chuanwise.xiaoming.minecraft.xiaoming.interactors;

import cn.chuanwise.xiaoming.annotation.Filter;
import cn.chuanwise.xiaoming.annotation.Required;
import cn.chuanwise.xiaoming.interactor.SimpleInteractors;
import cn.chuanwise.xiaoming.minecraft.xiaoming.Plugin;
import cn.chuanwise.xiaoming.minecraft.xiaoming.net.Server;
import cn.chuanwise.xiaoming.minecraft.xiaoming.configuration.PluginConfiguration;
import cn.chuanwise.xiaoming.minecraft.xiaoming.util.Words;
import cn.chuanwise.xiaoming.user.XiaomingUser;
import io.netty.channel.ChannelFuture;

import java.util.NoSuchElementException;
import java.util.Optional;

@SuppressWarnings("all")
public class ConnectionInteractors extends SimpleInteractors<Plugin> {
    @Filter(Words.ENABLE + Words.SERVER)
    @Required("xmmc.admin.server.enable")
    void enableServer(XiaomingUser user) {
        final Server server = plugin.getServer();
        final PluginConfiguration.Connection connection = plugin.getPluginConfiguration().getConnection();
        if (server.isBound()) {
            user.sendError("服务器已经启动了");
            return;
        }

        server.bind().orElseThrow(NoSuchElementException::new).addListener(x -> {
            if (x.isSuccess()) {
                user.sendMessage("成功在端口 " + connection.getPort() + " 上启动服务器");
            } else {
                user.sendError("未成功启动服务器");
            }
        });
    }

    @Filter(Words.DISABLE + Words.SERVER)
    @Required("xmmc.admin.server.enable")
    void disableServer(XiaomingUser user) {
        final Server server = plugin.getServer();
        if (!server.isBound()) {
            user.sendError("服务器并未启动");
            return;
        }

        server.unbind().orElseThrow(NoSuchElementException::new).addListener(x -> {
            if (x.isSuccess()) {
                user.sendMessage("成功关闭服务器");
            } else {
                user.sendError("未成功关闭服务器");
            }
        });
    }

    @Filter(Words.REENABLE + Words.SERVER)
    @Required("xmmc.admin.server.reenable")
    void reenableServer(XiaomingUser user) throws InterruptedException {
        final Server server = plugin.getServer();
        final Optional<ChannelFuture> optionalUnbindFuture = server.unbind();
        if (optionalUnbindFuture.isPresent()) {
            optionalUnbindFuture.get().sync();
        }

        final int port = plugin.getPluginConfiguration().getConnection().getPort();

        server.bind().orElseThrow(NoSuchElementException::new).addListener(x -> {
            if (x.isSuccess()) {
                user.sendMessage("成功在端口 " + port + " 上启动服务器");
            } else {
                user.sendError("未成功重启服务器");
            }
        });
    }
}