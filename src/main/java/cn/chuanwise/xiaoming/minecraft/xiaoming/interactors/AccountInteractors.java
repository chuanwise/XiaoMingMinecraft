package cn.chuanwise.xiaoming.minecraft.xiaoming.interactors;

import cn.chuanwise.common.util.CollectionUtil;
import cn.chuanwise.xiaoming.annotation.Filter;
import cn.chuanwise.xiaoming.annotation.FilterParameter;
import cn.chuanwise.xiaoming.annotation.Required;
import cn.chuanwise.xiaoming.interactor.SimpleInteractors;
import cn.chuanwise.xiaoming.minecraft.protocol.AskResponse;
import cn.chuanwise.xiaoming.minecraft.xiaoming.XMMCXiaoMingPlugin;
import cn.chuanwise.xiaoming.minecraft.xiaoming.configuration.PlayerConfiguration;
import cn.chuanwise.xiaoming.minecraft.xiaoming.configuration.PlayerInfo;
import cn.chuanwise.xiaoming.minecraft.xiaoming.net.OnlineClient;
import cn.chuanwise.xiaoming.minecraft.xiaoming.net.Server;
import cn.chuanwise.xiaoming.minecraft.xiaoming.util.Words;
import cn.chuanwise.xiaoming.user.XiaoMingUser;

import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.TimeoutException;

@SuppressWarnings("all")
public class AccountInteractors extends SimpleInteractors<XMMCXiaoMingPlugin> {
    @Filter(Words.PLAYER + " {qq}")
    @Filter(Words.PLAYER + Words.INFO + " {qq}")
    @Required("xmmc.admin.player.look")
    void playerInfo(XiaoMingUser user, @FilterParameter("qq") long qq) {
        final Optional<PlayerInfo> optionalPlayerInfo = plugin.getPlayerConfiguration().getPlayerInfo(qq);
        if (!optionalPlayerInfo.isPresent()) {
            user.sendError("该用户还没有绑定玩家名");
            return;
        }
        final PlayerInfo playerInfo = optionalPlayerInfo.get();

        user.sendMessage("「玩家信息」\n" +
                "QQ：" + CollectionUtil.toString(playerInfo.getAccountCodes()) + "\n" +
                "玩家名：" + CollectionUtil.toString(playerInfo.getPlayerNames()));
    }

    @Filter(Words.SERVER + Words.PLAYER + " {玩家名}")
    @Filter(Words.SERVER + Words.PLAYER + Words.INFO + " {玩家名}")
    @Required("xmmc.admin.player.look")
    void serverPlayer(XiaoMingUser user, @FilterParameter("玩家名") String playerName) {
        final Optional<PlayerInfo> optionalPlayerInfo = plugin.getPlayerConfiguration().getPlayerInfo(playerName);
        if (!optionalPlayerInfo.isPresent()) {
            user.sendError("该玩家还没有绑定 QQ");
            return;
        }
        final PlayerInfo playerInfo = optionalPlayerInfo.get();

        user.sendMessage("「玩家信息」\n" +
                "QQ：" + CollectionUtil.toString(playerInfo.getAccountCodes()) + "\n" +
                "玩家名：" + CollectionUtil.toString(playerInfo.getPlayerNames()));
    }

    @Filter(Words.MY + Words.PLAYER + Words.INFO + Words.INFO)
    @Filter(Words.MY + Words.PLAYER + Words.INFO)
    @Required("xmmc.user.player.look")
    void lookMyPlayerInfo(XiaoMingUser user) {
        final Optional<PlayerInfo> optionalPlayerInfo = plugin.getPlayerConfiguration().getPlayerInfo(user.getCode());
        if (!optionalPlayerInfo.isPresent()) {
            user.sendError("你还没有绑定玩家名");
            return;
        }
        final PlayerInfo playerInfo = optionalPlayerInfo.get();

        user.sendMessage("「你的信息」\n" +
                "QQ：" + CollectionUtil.toString(playerInfo.getAccountCodes()) + "\n" +
                "玩家名：" + CollectionUtil.toString(playerInfo.getPlayerNames()));

    }

    @Filter(Words.FORCE + Words.BIND + " {玩家名}")
    @Required("xmmc.admin.player.bind")
    void forceBind(XiaoMingUser user, @FilterParameter("玩家名") String playerName) {
        final PlayerConfiguration playerConfiguration = plugin.getPlayerConfiguration();

        switch (playerConfiguration.forceBind(user.getCode(), playerName)) {
            case SUCCEED:
                user.sendMessage("成功绑定玩家名「" + playerName + "」");
                playerConfiguration.readyToSave();
                return;
            case OTHER:
                user.sendMessage("玩家名「" + playerName + "」已被其他人绑定");
                break;
            case REPEAT:
                user.sendMessage("你已经绑定了这个玩家名");
                break;
            default:
                throw new NoSuchElementException();
        }
    }

    @Filter(Words.ENABLE + Words.BIND)
    @Required("xmmc.admin.player.bind.enable")
    void enableBind(XiaoMingUser user) {
        final PlayerConfiguration configuration = plugin.getPlayerConfiguration();

        if (configuration.isEnableBind()) {
            user.sendMessage("绑定已经开启了");
        } else {
            configuration.setEnableBind(true);
            configuration.readyToSave();

            user.sendMessage("成功开启绑定");
        }
    }

    @Filter(Words.DISABLE + Words.BIND)
    @Required("xmmc.admin.player.bind.disable")
    void disableBind(XiaoMingUser user) {
        final PlayerConfiguration configuration = plugin.getPlayerConfiguration();

        if (!configuration.isEnableBind()) {
            user.sendMessage("绑定并没有开启");
        } else {
            configuration.setEnableBind(false);
            configuration.readyToSave();

            user.sendMessage("成功关闭绑定");
        }
    }

    @Filter(Words.BIND + " {玩家名}")
    @Required("xmmc.user.player.bind")
    void bind(XiaoMingUser user, @FilterParameter("玩家名") String playerName) throws InterruptedException, TimeoutException {
        final PlayerConfiguration configuration = plugin.getPlayerConfiguration();

        if (!configuration.isEnableBind()) {
            if (user.hasPermission("xmmc.admin.player.bind")) {
                user.sendError("绑定不被允许，但你可以使用「强制绑定  " + playerName + "」以绑定");
            } else {
                user.sendError("绑定不被允许，请联系管理员开启绑定");
            }
            return;
        }

        // 检查玩家名是否被绑定
        final Optional<PlayerInfo> optionalSameNamePlayerInfo = configuration.getPlayerInfo(playerName);
        if (optionalSameNamePlayerInfo.isPresent()) {
            final PlayerInfo playerInfo = optionalSameNamePlayerInfo.get();
            if (playerInfo.hasAccountCode(user.getCode())) {
                user.sendMessage("你已经绑定了这个玩家名");
            } else {
                user.sendMessage("玩家名「" + playerName + "」已被其他人绑定");
            }
            return;
        }

        // 检查是否这个玩家在线
        final Server server = plugin.getServer();
        if (!server.isBound()) {
            user.sendError("服务器尚未启动，请联系管理员启动服务器");
            return;
        }

        OnlineClient onlineClient = null;
        for (OnlineClient client : server.getOnlineClients()) {
            if (client.getRemoteContact().playerIsOnline(playerName)) {
                onlineClient = client;
                break;
            }
        }
        if (Objects.isNull(onlineClient)) {
            user.sendError("我找不到这名用户，先上一个我在的服务器再尝试绑定吧");
            return;
        }

        user.sendMessage("请留意「" + onlineClient.getServerInfo().getName() + "」上的消息");

        // 在服务器上询问
        final AskResponse response = onlineClient.getServerClient().ask(playerName, user.getAliasAndCode() + "是你的 QQ 账号吗？这个账号申请将你绑定到他的 QQ 上", configuration.getBoundTimeout());
        switch (response) {
            case ACCEPTED:
                switch (configuration.forceBind(user.getCode(), playerName)) {
                    case REPEAT:
                        user.sendMessage("你已经绑定了玩家「" + playerName + "」");
                        break;
                    case OTHER:
                        user.sendMessage("玩家「" + playerName + "」刚刚已被其他人绑定");
                        break;
                    case SUCCEED:
                        user.sendMessage("成功绑定玩家「" + playerName + "」");
                        configuration.readyToSave();
                        break;
                    default:
                        throw new NoSuchElementException();
                }
                break;
            case IGNORED:
                user.sendError("对方忽略了你的绑定请求");
                break;
            case DENIED:
                user.sendError("对方拒绝了你的绑定请求");
                break;
            case TIMEOUT:
                user.sendError("绑定失败，你没有及时在服务器处理绑定请求");
                break;
            case CONFLICT:
                user.sendError("你在服务器还有待处理的请求，处理完后再绑定吧");
                break;
            case OFFLINE:
                user.sendError("玩家不在线，重新上一下服务器吧");
                break;
            default:
                throw new NoSuchElementException();
        }
    }

    @Filter(Words.UNBIND + " {玩家名}")
    @Required("xmmc.user.player.unbind")
    void unbind(XiaoMingUser user, @FilterParameter("玩家名") String playerName) {
        final PlayerConfiguration playerConfiguration = plugin.getPlayerConfiguration();
        final Optional<PlayerInfo> optionalPlayerInfo = playerConfiguration.getPlayerInfo(user.getCode());
        if (!optionalPlayerInfo.isPresent()) {
            user.sendError("你没有绑定任何玩家名哦");
            return;
        }
        final PlayerInfo playerInfo = optionalPlayerInfo.get();

        if (playerInfo.getPlayerNames().remove(playerName)) {
            user.sendMessage("成功解绑玩家名「" + playerName + "」");
            if (playerInfo.getPlayerNames().isEmpty()) {
                playerConfiguration.getPlayers().remove(playerInfo);
            }
            playerConfiguration.readyToSave();
        } else {
            user.sendMessage("你没有绑定玩家名「" + playerName + "」");
        }
    }

    @Filter(Words.FORCE + Words.UNBIND + " {qq} {玩家名}")
    @Required("xmmc.admin.player.unbind")
    void forceUnbind(XiaoMingUser user, @FilterParameter("qq") long qq, @FilterParameter("玩家名") String playerName) {
        final PlayerConfiguration configuration = plugin.getPlayerConfiguration();

        if (configuration.unbind(qq, playerName)) {
            configuration.readyToSave();
            user.sendMessage("成功解绑用户「" + xiaoMingBot.getAccountManager().getAliasOrCode(qq) + "」和玩家名「" + playerName + "」");
        } else {
            user.sendError("解绑失败，用户「" + xiaoMingBot.getAccountManager().getAliasOrCode(qq) + "」并没有绑定玩家名「" + playerName + "」");
        }
    }
}
