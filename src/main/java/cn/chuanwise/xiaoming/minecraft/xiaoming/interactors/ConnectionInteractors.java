package cn.chuanwise.xiaoming.minecraft.xiaoming.interactors;

import cn.chuanwise.xiaoming.annotation.Filter;
import cn.chuanwise.xiaoming.annotation.Required;
import cn.chuanwise.xiaoming.interactor.SimpleInteractors;
import cn.chuanwise.xiaoming.minecraft.xiaoming.Plugin;
import cn.chuanwise.xiaoming.minecraft.xiaoming.net.Server;
import cn.chuanwise.xiaoming.minecraft.xiaoming.configuration.PluginConfiguration;
import cn.chuanwise.xiaoming.minecraft.xiaoming.util.Words;
import cn.chuanwise.xiaoming.user.XiaomingUser;

@SuppressWarnings("all")
public class ConnectionInteractors extends SimpleInteractors<Plugin> {
    @Filter(Words.ENABLE + Words.SERVER)
    @Required("xmmc.admin.server.enable")
    void enableServer(XiaomingUser user) {
        final Server server = plugin.getServer();
        final PluginConfiguration.Connection connection = plugin.getPluginConfiguration().getConnection();
        if (server.isBound()) {
            user.sendError("服务器已经启动了！");
            return;
        }

        server.bind().orElseThrow().addListener(x -> {
            if (x.isSuccess()) {
                user.sendMessage("服务器成功在端口 " + connection.getPort() + " 上启动");
            } else {
                user.sendError("服务器启动失败");
            }
        });
    }

    @Filter(Words.DISABLE + Words.SERVER)
    @Required("xmmc.admin.server.enable")
    void disableServer(XiaomingUser user) {
        final Server server = plugin.getServer();
        if (!server.isBound()) {
            user.sendError("服务器并未启动！");
            return;
        }

        server.unbind().orElseThrow().addListener(x -> {
            if (x.isSuccess()) {
                user.sendMessage("服务器成功关闭");
            } else {
                user.sendError("服务器关闭失败");
            }
        });
    }
}
