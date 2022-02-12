package cn.chuanwise.xiaoming.minecraft.xiaoming.net;

import cn.chuanwise.mclib.net.protocol.NetLibProtocol;
import cn.chuanwise.mclib.net.protocol.SendSameMessageInform;
import cn.chuanwise.net.netty.packet.PacketHandler;
import cn.chuanwise.toolkit.container.Container;
import cn.chuanwise.util.CollectionUtil;
import cn.chuanwise.util.Preconditions;
import cn.chuanwise.util.StringUtil;
import cn.chuanwise.util.Times;
import cn.chuanwise.xiaoming.contact.contact.GroupContact;
import cn.chuanwise.xiaoming.contact.contact.XiaoMingContact;
import cn.chuanwise.xiaoming.contact.message.Message;
import cn.chuanwise.xiaoming.minecraft.protocol.*;
import cn.chuanwise.xiaoming.minecraft.xiaoming.XMMCXiaoMingPlugin;
import cn.chuanwise.xiaoming.minecraft.xiaoming.configuration.PlayerConfiguration;
import cn.chuanwise.xiaoming.minecraft.xiaoming.configuration.PlayerInfo;
import cn.chuanwise.xiaoming.minecraft.xiaoming.configuration.PlayerVerifyCodeConfiguration;
import cn.chuanwise.xiaoming.minecraft.xiaoming.configuration.StringGenerator;
import cn.chuanwise.xiaoming.minecraft.xiaoming.event.*;
import cn.chuanwise.xiaoming.object.PluginObjectImpl;
import lombok.Data;

import java.util.*;
import java.util.concurrent.TimeoutException;

@Data
@SuppressWarnings("all")
public class XMMCServerClient extends PluginObjectImpl<XMMCXiaoMingPlugin> {
    protected final PacketHandler packetHandler;
    protected final OnlineClient onlineClient;

    public XMMCServerClient(OnlineClient onlineClient, PacketHandler packetHandler) {
        setPlugin(onlineClient.getPlugin());
        Preconditions.nonNull(packetHandler, "packet handler");

        this.packetHandler = packetHandler;
        this.onlineClient = onlineClient;

        setupBaseListeners();
        setupTriggerForwarders();
        setupPlayerInfoListeners();
    }

    private void setupBaseListeners() {
        packetHandler.setOnRequest(XMMCProtocol.REQUEST_CONFIRM_ACTIVE, v -> v);
        packetHandler.setOnInform(XMMCProtocol.INFORM_MESSAGE, argument -> {
            plugin.getXiaoMingBot().getEventManager().callEvent(new ServerMessageEvent(onlineClient, argument));
        });
    }

    public void sendMessage(String message) {
        packetHandler.inform(XMMCProtocol.INFORM_MESSAGE, message);
    }

    public void sendWideMessage(Set<String> playerNames, String message) {
        packetHandler.inform(XMMCProtocol.INFORM_WIDE_MESSAGE, new SendSameMessageInform(playerNames, message));
    }

    private void setupTriggerForwarders() {
        final Server server = plugin.getServer();

        packetHandler.setOnInform(NetLibProtocol.INFORM_PLAYER_CHANGE_WORLD_EVENT, x -> {
            final PlayerChangeWorldEvent event = new PlayerChangeWorldEvent(x, onlineClient);
            xiaoMingBot.getEventManager().callEvent(event);
        });
        packetHandler.setOnInform(XMMCProtocol.INFORM_TPS, x -> {
            final ServerTpsEvent event = new ServerTpsEvent(x, onlineClient);
            xiaoMingBot.getEventManager().callEvent(event);
        });
        packetHandler.setOnRequest(NetLibProtocol.REQUEST_PLAYER_CHAT_EVENT, x -> {
            final PlayerChatEvent event = new PlayerChatEvent(x, onlineClient);
            xiaoMingBot.getEventManager().callEvent(event);
            return event.isCancelled();
        });
        packetHandler.setOnInform(NetLibProtocol.INFORM_PLAYER_JOIN_EVENT, x -> {
            final PlayerJoinEvent event = new PlayerJoinEvent(x, onlineClient);
            xiaoMingBot.getEventManager().callEvent(event);
        });
        packetHandler.setOnInform(NetLibProtocol.INFORM_PLAYER_QUIT_EVENT, x -> {
            final PlayerQuitEvent event = new PlayerQuitEvent(x, onlineClient);
            xiaoMingBot.getEventManager().callEvent(event);
        });
        packetHandler.setOnInform(NetLibProtocol.INFORM_PLAYER_DEATH_EVENT, x -> {
            final PlayerDeathEvent event = new PlayerDeathEvent(x, onlineClient);
            xiaoMingBot.getEventManager().callEvent(event);
        });
        packetHandler.setOnRequest(NetLibProtocol.REQUEST_PLAYER_KICK_EVENT, x -> {
            final PlayerKickEvent event = new PlayerKickEvent(x, onlineClient);
            xiaoMingBot.getEventManager().callEvent(event);
            return event.isCancelled();
        });
    }

    private void setupPlayerInfoListeners() {
        packetHandler.setOnRequest(XMMCProtocol.REQUEST_PLAYER_BIND_INFO, request -> {
            return plugin.getPlayerConfiguration().getPlayerInfo(request)
                    .map(x -> {
                        return new PlayerBindInfo(x.getPlayerNames(), x.getAccountCodes());
                    })
                    .orElse(null);
        });
        packetHandler.setOnRequest(XMMCProtocol.REQUEST_PLAYER_VERIFY_CODE, playerName -> {
            // 如果已经绑定了，就不分配了
            if (plugin.getPlayerConfiguration().getPlayerInfo(playerName).isPresent()) {
                return null;
            }

            // 否则分配一个值，并回复
            final PlayerVerifyCodeConfiguration configuration = plugin.getPlayerVerifyCodeConfiguration();
            final PlayerVerifyCodeInfo info;

            // 先查找是否有现成的
            final Map<String, PlayerVerifyCodeConfiguration.VerifyInfo> verifyInfo = configuration.getVerifyInfo();
            final Container<Map.Entry<String, PlayerVerifyCodeConfiguration.VerifyInfo>> container =
                    CollectionUtil.findFirst(verifyInfo.entrySet(), x -> Objects.equals(x.getValue().getPlayerName(), playerName));

            String verifyCode;
            if (container.isEmpty()) {
                final StringGenerator generator = plugin.getBaseConfiguration().getGenerator().getVerifyCode();
                verifyCode = StringUtil.randomString(generator.getCharacters(), generator.getLength());
                while (verifyInfo.containsKey(verifyCode)) {
                    verifyCode = StringUtil.randomString(generator.getCharacters(), generator.getLength());
                }

                verifyInfo.put(verifyCode, new PlayerVerifyCodeConfiguration.VerifyInfo(System.currentTimeMillis(), playerName));
            } else {
                final Map.Entry<String, PlayerVerifyCodeConfiguration.VerifyInfo> entry = container.get();
                verifyCode = entry.getKey();
                entry.getValue().setTimeMillis(System.currentTimeMillis());
            }
            configuration.readyToSave();
            return new PlayerVerifyCodeInfo(verifyCode, configuration.getTimeout());
        });

        packetHandler.setOnRequest(XMMCProtocol.REQUEST_PLAYER_BIND, request -> {
            final String playerName = request.getPlayerName();
            final long accountCode = request.getAccountCode();
            final PlayerConfiguration playerConfiguration = plugin.getPlayerConfiguration();

            // 检查是否重复绑定
            final Optional<PlayerInfo> optionalSameCodePlayerInfo = playerConfiguration.getPlayerInfo(accountCode);
            if (optionalSameCodePlayerInfo.isPresent()) {
                final PlayerInfo sameCodePlayerInfo = optionalSameCodePlayerInfo.get();
                if (sameCodePlayerInfo.hasPlayerName(playerName)) {
                    return new PlayerBindResponse.Error(PlayerBindResponse.Error.Type.REPEAT);
                }
            }

            // 检查名字是否被绑定
            final Optional<PlayerInfo> optionalSameNamePlayerInfo = playerConfiguration.getPlayerInfo(playerName);
            if (optionalSameNamePlayerInfo.isPresent()) {
                final PlayerInfo sameNamePlayerInfo = optionalSameNamePlayerInfo.get();
                Preconditions.state(!sameNamePlayerInfo.hasAccountCode(accountCode), "internal error");
                return new PlayerBindResponse.Error(PlayerBindResponse.Error.Type.OTHER);
            }

            // 找到这个用户
            final long timeout = playerConfiguration.getBoundTimeout();
            final String timeoutLength = Times.toTimeLength(timeout);
            final String message = "位于「" + onlineClient.getServerInfo().getName() + "」上的玩家「" + playerName + "」是你吗？\n" +
                    "这名玩家请求绑定在你的 QQ 上。同意绑定，请在 " + timeoutLength + " 内回复「绑定」。\n" +
                    "超时或其他任何回复将取消绑定";
            final Optional<XiaoMingContact> optionalContact = plugin.getXiaoMingBot().getContactManager().sendMessagePossibly(accountCode, message);
            if (!optionalContact.isPresent()) {
                return new PlayerBindResponse.Error(PlayerBindResponse.Error.Type.FAILED);
            }
            final XiaoMingContact xiaomingContact = optionalContact.get();

            // 启动新线程等待结果
            xiaoMingBot.getScheduler().run(() -> {
                PlayerBindResultInform inform;
                try {
                    boolean bound = false;
                    if (xiaomingContact instanceof GroupContact) {
                        final Optional<Message> optionalMessage = ((GroupContact) xiaomingContact).getMember(accountCode).orElseThrow(NoSuchElementException::new).nextMessage(timeout);
                        if (!optionalMessage.isPresent()) {
                            ((GroupContact) xiaomingContact).atSend(accountCode, "你没有及时处理绑定请求，请求已取消");
                            inform = PlayerBindResultInform.TIMEOUT;
                        } else {
                            bound = Objects.equals(optionalMessage.get().serialize(), "绑定");
                            if (bound) {
                                ((GroupContact) xiaomingContact).atSend(accountCode, "成功绑定玩家「" + playerName + "」");
                            } else {
                                ((GroupContact) xiaomingContact).atSend(accountCode, "已拒绝绑定请求");
                            }
                        }
                    } else {
                        final Optional<Message> optionalMessage = xiaomingContact.nextMessage(accountCode);
                        if (!optionalMessage.isPresent()) {
                            xiaomingContact.sendWarning("你没有及时处理绑定请求，请求已取消");
                            inform = PlayerBindResultInform.TIMEOUT;
                        } else {
                            bound = Objects.equals(optionalMessage.get().serialize(), "绑定");
                            if (bound) {
                                xiaomingContact.sendMessage("成功绑定玩家「" + playerName + "」");
                            } else {
                                xiaomingContact.sendMessage("已拒绝绑定请求");
                            }
                        }
                    }

                    if (bound) {
                        playerConfiguration.forceBind(accountCode, playerName);
                        playerConfiguration.readyToSave();
                        inform = PlayerBindResultInform.ACCEPTED;
                    } else {
                        inform = PlayerBindResultInform.DENIED;
                    }
                } catch (InterruptedException exception) {
                    inform = PlayerBindResultInform.INTERRUPTED;
                }
                packetHandler.inform(XMMCProtocol.INFORM_PLAYER_BIND_RESULT, inform);
            });
            return new PlayerBindResponse.Wait(xiaomingContact.getAliasAndCode(), timeout);
        });

        packetHandler.setOnRequest(XMMCProtocol.REQUEST_PLAYER_UNBIND, playerName -> {
            final PlayerConfiguration playerConfiguration = plugin.getPlayerConfiguration();
            final Optional<PlayerInfo> optionalPlayerInfo = playerConfiguration.getPlayerInfo(playerName);
            if (!optionalPlayerInfo.isPresent()) {
                return false;
            }
            final PlayerInfo playerInfo = optionalPlayerInfo.get();

            return playerConfiguration.unbind(playerName);
        });
    }

    public Set<OnlinePlayerResponse.PlayerKey> getOnlinePlayerKeys() throws InterruptedException, TimeoutException {
        return packetHandler.obtain(XMMCProtocol.OBTAIN_ONLINE_PLAYERS).getPlayerKeys();
    }

    public AskResponse ask(String playerName, String message, long timeout) throws InterruptedException, TimeoutException {
        return packetHandler.request(XMMCProtocol.REQUEST_ASK, new AskRequest(playerName, message, timeout), timeout);
    }

    public boolean confirmActive() throws InterruptedException {
        boolean active;
        try {
            final long timeMillis = System.currentTimeMillis();
            active = timeMillis == packetHandler.request(XMMCProtocol.REQUEST_CONFIRM_ACTIVE, timeMillis);
        } catch (TimeoutException e) {
            active = false;
        }
        if (!active) {
            packetHandler.getContext().channel().close();
        }
        return active;
    }
}
