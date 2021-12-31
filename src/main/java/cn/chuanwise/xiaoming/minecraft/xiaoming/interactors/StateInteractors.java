package cn.chuanwise.xiaoming.minecraft.xiaoming.interactors;

import cn.chuanwise.toolkit.container.Container;
import cn.chuanwise.util.CollectionUtil;
import cn.chuanwise.xiaoming.annotation.Filter;
import cn.chuanwise.xiaoming.annotation.FilterParameter;
import cn.chuanwise.xiaoming.annotation.Required;
import cn.chuanwise.xiaoming.interactor.SimpleInteractors;
import cn.chuanwise.xiaoming.minecraft.xiaoming.OnlineClient;
import cn.chuanwise.xiaoming.minecraft.xiaoming.Plugin;
import cn.chuanwise.xiaoming.minecraft.xiaoming.Server;
import cn.chuanwise.xiaoming.minecraft.xiaoming.configuration.Configuration;
import cn.chuanwise.xiaoming.minecraft.xiaoming.configuration.ServerInfo;
import cn.chuanwise.xiaoming.minecraft.xiaoming.util.Words;
import cn.chuanwise.xiaoming.user.XiaomingUser;

import java.util.List;
import java.util.Map;
import java.util.Objects;

@SuppressWarnings("all")
public class StateInteractors extends SimpleInteractors<Plugin> {
    public Server getServer() {
        return plugin.getServer();
    }

    @Filter(Words.ONLINE + Words.SERVER)
    @Required("xmmc.admin.onlineServers")
    void onlineServers(XiaomingUser user) {
        final Server server = getServer();

        if (!server.isBound()) {
            user.sendError("服务器尚未启动，考虑使用「启动服务器」启动");
            return;
        }
        final List<OnlineClient> onlineClients = server.getOnlineClients();

    }

    @Filter(Words.DISCONNECT + Words.SERVER + " {服务器名}")
    @Required("xmmc.admin.online.disconnect")
    void disconnectOnlineClient(XiaomingUser user, @FilterParameter("服务器名") OnlineClient onlineClient) {
        final ServerInfo serverInfo = onlineClient.getServerInfo();
        onlineClient.disconnect().addListener(x -> {
            if (x.isSuccess()) {
                user.sendMessage("成功断开了和「" + serverInfo.getName() + "」的连接");
            } else {
                user.sendMessage("未成功断开了和「" + serverInfo.getName() + "」的连接");
            }
        });
    }

    @Filter(Words.SERVER)
    @Required("xmmc.admin.server.list")
    void listServers(XiaomingUser user) {
        final Map<String, ServerInfo> servers = plugin.getConfiguration().getServers();
        if (servers.isEmpty()) {
            user.sendError("小明还不认识任何服务器");
        } else {
            user.sendMessage("小明认识 " + servers.size() + " 个服务器：\n" +
                    CollectionUtil.toIndexString(servers.values(), ServerInfo::getName));
        }
    }

    @Filter(Words.REMOVE + Words.SERVER + " {服务器名}")
    @Required("xmmc.admin.server.remove")
    void removeServer(XiaomingUser user, @FilterParameter("服务器名") ServerInfo serverInfo) {
        final Configuration configuration = plugin.getConfiguration();
        configuration.getServers().remove(serverInfo.getName());
        configuration.readyToSave();

        // 尝试断开
        final Server server = getServer();
        final Container<OnlineClient> optionalOnlineClient = CollectionUtil.findFirst(server.getOnlineClients(), x -> Objects.equals(x.getServerInfo(), serverInfo));

        if (optionalOnlineClient.notEmpty()) {
            optionalOnlineClient.get().disconnect().addListener(x -> {
                if (x.isSuccess()) {
                    user.sendMessage("成功删除服务器「" + serverInfo + "」，但未断开与之的连接");
                } else {
                    user.sendMessage("成功删除服务器「" + serverInfo + "」，并断开与之的连接");
                }
            });
        } else {
            user.sendMessage("成功删除服务器「" + serverInfo + "」");
        }
    }
}