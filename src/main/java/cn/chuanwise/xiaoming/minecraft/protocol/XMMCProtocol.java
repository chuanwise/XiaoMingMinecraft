package cn.chuanwise.xiaoming.minecraft.protocol;

import cn.chuanwise.mclib.net.protocol.NetLibProtocol;
import cn.chuanwise.net.Protocol;
import cn.chuanwise.net.netty.BaseProtocol;
import cn.chuanwise.net.netty.ProtocolException;
import cn.chuanwise.net.packet.InformPacketType;
import cn.chuanwise.net.packet.ObtainPacketType;
import cn.chuanwise.net.packet.RequestPacketType;

/** XMMC 通讯协议 */
public class XMMCProtocol extends Protocol {
    /** 通讯协议版本 */
    public static final String VERSION = "1.0-exp";

    public static final RequestPacketType<Long, Long> REQUEST_CONFIRM_ACTIVE = new RequestPacketType<>(Long.class, Long.class);

    /**
     * 如果回应 {@link VerifyResponse.Confirm}，则需要用户在 qq 上确认特征信息。
     * 当确认成功时，服务器发送 {@link #REQUEST_CONFIRM} 请求，回应 true 表示成功，false 表示失败。
     */
    public static final RequestPacketType<VerifyRequest, VerifyResponse> REQUEST_VERIFY = new RequestPacketType<>(VerifyRequest.class, VerifyResponse.class);
    public static final RequestPacketType<ConfirmRequest, Boolean> REQUEST_CONFIRM = new RequestPacketType<>(ConfirmRequest.class, Boolean.class);

    /** 发送世界消息和服务器消息 */
    public static final InformPacketType<SendWorldMessageRequest> INFORM_WORLD_MESSAGE = new InformPacketType<>(SendWorldMessageRequest.class);

    public static final ObtainPacketType<OnlinePlayerResponse> OBTAIN_ONLINE_PLAYERS = new ObtainPacketType<>(OnlinePlayerResponse.class);

    public static final RequestPacketType<AskRequest, AskResponse> REQUEST_ASK = new RequestPacketType<>(AskRequest.class, AskResponse.class);

    /** 绑定相关 */
    public static final RequestPacketType<PlayerBindRequest, PlayerBindResponse> REQUEST_PLAYER_BIND = new RequestPacketType<>(PlayerBindRequest.class, PlayerBindResponse.class);
    public static final RequestPacketType<String, Boolean> REQUEST_PLAYER_UNBIND = new RequestPacketType<>(String.class, Boolean.class);
    public static final InformPacketType<PlayerBindResultInform> INFORM_PLAYER_BIND_RESULT = new InformPacketType<>(PlayerBindResultInform.class);

    public static void checkProtocol(boolean legal, String message) {
        if (legal) {
            return;
        }
        throw new ProtocolException("通讯协议错误：" + message + "（协议版本：" + VERSION + "）");
    }

    public static void checkProtocol(boolean legal) {
        if (legal) {
            return;
        }
        throw new ProtocolException("通讯协议错误（协议版本：" + VERSION + "）");
    }

    private static final XMMCProtocol INSTANCE = new XMMCProtocol();
    public static XMMCProtocol getInstance() {
        return INSTANCE;
    }
    private XMMCProtocol() {
        registerStaticPacketTypes(XMMCProtocol.class);
        registerStaticPacketTypes(BaseProtocol.class);
        registerStaticPacketTypes(NetLibProtocol.class);
    }
}