package cn.chuanwise.xiaoming.minecraft.xiaoming.net;

import cn.chuanwise.api.Flushable;
import cn.chuanwise.mclib.net.contact.NetLibRemoteContact;
import cn.chuanwise.net.netty.protocol.BaseProtocol;
import cn.chuanwise.net.netty.packet.PacketHandler;
import cn.chuanwise.net.packet.SignPacket;
import cn.chuanwise.util.CollectionUtil;
import cn.chuanwise.util.Preconditions;
import cn.chuanwise.xiaoming.minecraft.protocol.SendWorldMessageRequest;
import cn.chuanwise.xiaoming.minecraft.protocol.XMMCProtocol;
import cn.chuanwise.xiaoming.minecraft.xiaoming.XMMCXiaoMingPlugin;
import cn.chuanwise.xiaoming.minecraft.xiaoming.configuration.ServerInfo;
import cn.chuanwise.xiaoming.minecraft.xiaoming.event.ServerDisconnectedEvent;
import cn.chuanwise.xiaoming.object.PluginObjectImpl;
import io.netty.channel.*;
import io.netty.handler.timeout.IdleStateEvent;
import lombok.Getter;

import java.io.IOException;
import java.util.List;
import java.util.Objects;
import java.util.Set;

@Getter
public class OnlineClient
        extends PluginObjectImpl<XMMCXiaoMingPlugin>
        implements Flushable {
    protected final Channel channel;

    protected final Server server;
    protected final ServerInfo serverInfo;

    protected final long connectTimeMillis = System.currentTimeMillis();

    protected final PacketHandler packetHandler;
    protected final NetLibRemoteContact remoteContact;
    protected final XMMCServerClient serverClient;

    /** 心跳发送器 */
    @ChannelHandler.Sharable
    protected class HeartbeatHandler extends ChannelInboundHandlerAdapter {
        volatile long lastHeartbeatTimeMillis = System.currentTimeMillis();

        @Override
        public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
            if (!(msg instanceof SignPacket)) {
                super.channelRead(ctx, msg);
                return;
            }
            final SignPacket signPacket = (SignPacket) msg;

            if (!Objects.equals(signPacket.getPacketType(), BaseProtocol.HEART_BEAT)) {
                super.channelRead(ctx, msg);
                return;
            }

            lastHeartbeatTimeMillis = System.currentTimeMillis();
        }

        @Override
        public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
            if (evt instanceof IdleStateEvent) {
                plugin.getLogger().warn("服务器「" + serverInfo.getName() + "」掉线");
                ctx.channel().close();
            } else {
                super.userEventTriggered(ctx, evt);
            }
        }
    }
    protected final HeartbeatHandler heartbeatHandler = new HeartbeatHandler();

    /** 异常掉线器 */
    @ChannelHandler.Sharable
    protected class ExceptionHandler extends ChannelInboundHandlerAdapter {
        @Override
        public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
            ctx.channel().close();
            if (cause instanceof ConnectTimeoutException) {
                plugin.getLogger().warn("连接超时");
                return;
            }
            if (cause instanceof IOException) {
                plugin.getLogger().warn("连接出现异常");
                return;
            }
            plugin.getLogger().warn("连接出现异常", cause);
        }
    }
    protected final ExceptionHandler exceptionHandler = new ExceptionHandler();

    @ChannelHandler.Sharable
    protected class DisconnectHandler extends ChannelInboundHandlerAdapter {
        @Override
        public void handlerRemoved(ChannelHandlerContext ctx) throws Exception {
            plugin.getXiaoMingBot().getEventManager().callEvent(new ServerDisconnectedEvent(OnlineClient.this));
            super.handlerRemoved(ctx);
        }
    }
    protected final DisconnectHandler disconnectHandler = new DisconnectHandler();

    public OnlineClient(Server server, Channel channel, ServerInfo serverInfo) {
        Preconditions.nonNull(server, "server");
        Preconditions.nonNull(serverInfo, "server info");
        Preconditions.nonNull(channel, "channel");

        this.server = server;
        this.serverInfo = serverInfo;
        this.channel = channel;
        this.packetHandler = new PacketHandler();
        setPlugin(server.getPlugin());

        channel.pipeline().addLast(heartbeatHandler);
        channel.pipeline().addLast(packetHandler);
        channel.pipeline().addLast(exceptionHandler);
        channel.pipeline().addLast(disconnectHandler);

        remoteContact = new NetLibRemoteContact(packetHandler);
        serverClient = new XMMCServerClient(this, packetHandler);

        serverInfo.setLastConnectTimeMillis(System.currentTimeMillis());
        plugin.getSessionConfiguration().readyToSave();

        packetHandler.setDefaultMessageListener(packet -> {
            plugin.getLog().debug("no type packet: " + packet);
        });
    }

    public ChannelFuture disconnect() {
        server.onlineClients.remove(this);
        return channel.close();
    }

    public void sendWorldMessage(Set<String> worldNames, List<String> message) {
        worldNames = CollectionUtil.copyOf(worldNames);
        message = CollectionUtil.copyOf(message);
        packetHandler.inform(XMMCProtocol.INFORM_WORLD_MESSAGE, new SendWorldMessageRequest(worldNames, message));
    }

    public boolean isConnected() {
        return channel.isActive();
    }

    @Override
    public void flush() {
        if (!isConnected()) {
            disconnect();
        }
    }
}