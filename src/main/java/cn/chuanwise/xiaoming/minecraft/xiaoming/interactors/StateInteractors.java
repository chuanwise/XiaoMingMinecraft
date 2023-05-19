package cn.chuanwise.xiaoming.minecraft.xiaoming.interactors;

import cn.chuanwise.mclib.util.Colors;
import cn.chuanwise.common.place.Container;
import cn.chuanwise.common.util.CollectionUtil;
import cn.chuanwise.common.util.Times;
import cn.chuanwise.xiaoming.annotation.Filter;
import cn.chuanwise.xiaoming.annotation.FilterParameter;
import cn.chuanwise.xiaoming.annotation.Required;
import cn.chuanwise.xiaoming.interactor.SimpleInteractors;
import cn.chuanwise.xiaoming.minecraft.protocol.OnlinePlayerResponse;
import cn.chuanwise.xiaoming.minecraft.xiaoming.XMMCXiaoMingPlugin;
import cn.chuanwise.xiaoming.minecraft.xiaoming.net.OnlineClient;
import cn.chuanwise.xiaoming.minecraft.xiaoming.net.Server;
import cn.chuanwise.xiaoming.minecraft.xiaoming.configuration.SessionConfiguration;
import cn.chuanwise.xiaoming.minecraft.xiaoming.configuration.ServerInfo;
import cn.chuanwise.xiaoming.minecraft.xiaoming.util.Words;
import cn.chuanwise.xiaoming.user.XiaoMingUser;

import java.util.*;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

@SuppressWarnings("all")
public class StateInteractors extends SimpleInteractors<XMMCXiaoMingPlugin> {
    public Server getServer() {
        return plugin.getServer();
    }

    @Filter(Words.ONLINE + Words.SERVER)
    @Required("xmmc.admin.onlineServers")
    void onlineServers(XiaoMingUser user) {
        final Server server = getServer();

        if (!server.isBound()) {
            user.sendError("服务器尚未启动，考虑使用「启动服务器」启动");
            return;
        }
        final List<OnlineClient> onlineClients = server.getOnlineClients();
        if (onlineClients.isEmpty()) {
            user.sendError("目前没有任何服务器在线");
        } else if (onlineClients.size() == 1) {
            final OnlineClient onlineClient = onlineClients.get(0);
            user.sendMessage("目前只有「" + onlineClient.getServerInfo().getName() + "」在线，" +
                    "已在线 " + Times.toTimeLength(System.currentTimeMillis() - onlineClient.getConnectTimeMillis()));
        } else {
            user.sendMessage("在线服务器：\n" +
                    CollectionUtil.toIndexString(onlineClients, x -> x.getServerInfo().getName() +
                            "（已在线 " + Times.toTimeLength(System.currentTimeMillis() - x.getConnectTimeMillis()) + "）"));
        }
    }

    @Filter(Words.DISCONNECT + Words.SERVER + " {服务器名}")
    @Required("xmmc.admin.online.disconnect")
    void disconnectOnlineClient(XiaoMingUser user, @FilterParameter("服务器名") OnlineClient onlineClient) {
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
    void listServers(XiaoMingUser user) {
        final Map<String, ServerInfo> servers = plugin.getSessionConfiguration().getServers();
        if (servers.isEmpty()) {
            user.sendError("小明还不认识任何服务器");
        } else {
            user.sendMessage("小明认识 " + servers.size() + " 个服务器：\n" +
                    CollectionUtil.toIndexString(servers.values(), x -> {
                        final String lastOnlineInfo = x.getLastConnectTimeMillis() == 0 ? "从未连接" : ("上次在线是" + Times.beforeTime(x.getLastConnectTimeMillis()));
                        final String message = x.getName() + "（" + lastOnlineInfo + "）";
                        return message;
                    }));
        }
    }

    @Filter(Words.SERVER + " {服务器}")
    @Required("xmmc.admin.server.look")
    void serverInfo(XiaoMingUser user, @FilterParameter("服务器") ServerInfo serverInfo) {
        user.sendMessage("「服务器信息」\n" +
                "服务器名：" + serverInfo.getName() + "\n" +
                "注册时间：" + Times.beforeTime(serverInfo.getRegisterTimeMillis()) + "\n" +
                "上次在线：" + (serverInfo.getLastConnectTimeMillis() == 0 ? "（从未连接过）" : Times.beforeTime(serverInfo.getLastConnectTimeMillis())) + "\n" +
                "标签：" + serverInfo.getTags()
        );
    }

    @Filter(Words.REMOVE + Words.SERVER + " {服务器名}")
    @Required("xmmc.admin.server.remove")
    void removeServer(XiaoMingUser user, @FilterParameter("服务器名") ServerInfo serverInfo) {
        final SessionConfiguration sessionConfiguration = plugin.getSessionConfiguration();
        sessionConfiguration.getServers().remove(serverInfo.getName());
        sessionConfiguration.readyToSave();

        // 尝试断开
        final Server server = getServer();
        final Container<OnlineClient> optionalOnlineClient = CollectionUtil.findFirst(server.getOnlineClients(), x -> Objects.equals(x.getServerInfo(), serverInfo));

        if (optionalOnlineClient.isSet()) {
            optionalOnlineClient.get().disconnect().addListener(x -> {
                if (x.isSuccess()) {
                    user.sendMessage("成功删除服务器「" + serverInfo.getName() + "」，但未断开与之的连接");
                } else {
                    user.sendMessage("成功删除服务器「" + serverInfo.getName() + "」，并断开与之的连接");
                }
            });
        } else {
            user.sendMessage("成功删除服务器「" + serverInfo.getName() + "」");
        }
    }

    @Filter(Words.ONLINE + Words.PLAYER + " {服务器名}")
    @Filter(Words.ONLINE + Words.POPULATION + " {服务器名}")
    @Filter(Words.PLAYER + Words.LIST + " {服务器名}")
    @Filter(Words.ALL + Words.PLAYER + " {服务器名}")
    @Required("xmmc.user.onlinePlayers")
    void onlinePlayers(XiaoMingUser user, @FilterParameter("服务器名") OnlineClient onlineClient) throws InterruptedException, TimeoutException {
        final Set<OnlinePlayerResponse.PlayerKey> onlinePlayerKeys = onlineClient.getServerClient().getOnlinePlayerKeys();
        if (onlinePlayerKeys.isEmpty()) {
            user.sendError("服务器上没有任何人哦");
            return;
        }

        final List<String> playerNames = onlinePlayerKeys.stream()
                .map(OnlinePlayerResponse.PlayerKey::getPlayerName)
                .collect(Collectors.toList());

        user.sendMessage("服务器上有 " + playerNames.size() + " 个人：\n" +
                Colors.clearColors(CollectionUtil.toIndexString(playerNames)));
    }

    @Filter(Words.ONLINE + Words.PLAYER)
    @Filter(Words.ONLINE + Words.POPULATION)
    @Filter(Words.PLAYER + Words.LIST)
    @Filter(Words.ALL + Words.PLAYER)
    @Required("xmmc.user.onlinePlayers")
    void allOnlinePlayers(XiaoMingUser user) throws InterruptedException, TimeoutException {
        final Server server = getServer();
        if (!server.isBound()) {
            user.sendError("服务器尚未启动！");
            return;
        }

        final List<OnlineClient> onlineClients = server.getOnlineClients();
        if (onlineClients.isEmpty()) {
            user.sendError("小明尚未连接到任何服务器！");
            return;
        }

        final Map<String, List<String>> receipt = new HashMap<>();
        for (OnlineClient onlineClient : onlineClients) {
            final List<String> playerNames = onlineClient.getServerClient()
                    .getOnlinePlayerKeys()
                    .stream()
                    .map(OnlinePlayerResponse.PlayerKey::getPlayerDisplayName)
                    .collect(Collectors.toList());
            receipt.put(onlineClient.getServerInfo().getName(), playerNames);
        }

        user.sendMessage(Colors.clearColors(CollectionUtil.toIndexString(receipt.entrySet(), x -> x.getKey() + "：" +
                Optional.ofNullable(CollectionUtil.toString(x.getValue())).orElse("（无）"))));
    }
}