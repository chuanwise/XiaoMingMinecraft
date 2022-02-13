package cn.chuanwise.xiaoming.minecraft.bukkit.net;

import cn.chuanwise.mclib.bukkit.net.contact.BukkitLocalContact;
import cn.chuanwise.mclib.bukkit.plugin.BukkitPluginObject;
import cn.chuanwise.net.netty.event.*;
import cn.chuanwise.net.netty.exception.ProtocolException;
import cn.chuanwise.net.netty.handler.DebugDuplexHandler;
import cn.chuanwise.net.netty.handler.ReconnectStateHandler;
import cn.chuanwise.net.netty.packet.JsonPacketCodec;
import cn.chuanwise.net.netty.packet.PacketHandler;
import cn.chuanwise.net.netty.protocol.BaseProtocol;
import cn.chuanwise.net.packet.*;
import cn.chuanwise.toolkit.box.Box;
import cn.chuanwise.toolkit.container.Container;
import cn.chuanwise.util.Throwables;
import cn.chuanwise.util.Times;
import cn.chuanwise.xiaoming.minecraft.bukkit.XMMCBukkitPlugin;
import cn.chuanwise.xiaoming.minecraft.bukkit.configuration.ConnectionConfiguration;
import cn.chuanwise.xiaoming.minecraft.protocol.ConfirmRequest;
import cn.chuanwise.xiaoming.minecraft.protocol.VerifyRequest;
import cn.chuanwise.xiaoming.minecraft.protocol.VerifyResponse;
import cn.chuanwise.xiaoming.minecraft.util.PasswordHashUtil;
import cn.chuanwise.xiaoming.minecraft.protocol.XMMCProtocol;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import io.netty.handler.codec.LengthFieldPrepender;
import io.netty.handler.codec.string.StringDecoder;
import io.netty.handler.codec.string.StringEncoder;
import io.netty.handler.timeout.IdleStateEvent;
import io.netty.handler.timeout.IdleStateHandler;
import io.netty.util.concurrent.Future;
import lombok.AccessLevel;
import lombok.Getter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Predicate;

@Getter
public class Client
        extends BukkitPluginObject<XMMCBukkitPlugin> {
    /** 连接的引导器 */
    protected final Bootstrap bootstrap = new Bootstrap();

    protected PacketHandler packetHandler = new PacketHandler();

    /** 本地会话处理器 */
    protected final BukkitLocalContact<XMMCBukkitPlugin> localContact = new BukkitLocalContact<>(packetHandler, communicator().toLogger(), plugin);
    protected final XMMCClientContact clientContact = new XMMCClientContact(plugin, packetHandler);

    /** 重连工具 */
    protected NioEventLoopGroup executors;

    @Getter(AccessLevel.NONE)
    protected Channel channel;

    @Getter(AccessLevel.NONE)
    private final Object verifyCondition = new Object();

    protected volatile String name;

    /** 负责指导重连操作的 Handler */
    private final ReconnectStateHandler reconnectStateHandler = new ReconnectStateHandler();

    /**
     * 专门负责客户端连接的 Handler
     * 其他任何地方都禁止调用 {@code bootstrap.connect()}
     * 如需连接，应该调用 {@link ConnectHandler#connect()}
     */
    @ChannelHandler.Sharable
    private class ConnectHandler extends ChannelInboundHandlerAdapter {
        /** 检测连接状态变化的量 */
        private volatile boolean connected;

        @Override
        public void channelActive(ChannelHandlerContext context) throws Exception {
            connected = true;
            channel = context.channel();
            super.channelActive(context);
        }

        @Override
        public void channelInactive(ChannelHandlerContext context) throws Exception {
            connected = false;
            channel = null;
            super.channelInactive(context);
        }

        private ChannelFuture connectFuture;

        /**
         * 唯一的连接方法
         * @return 如果已经连接到服务器了，返回 {@link Optional#empty()}
         */
        public Optional<ChannelFuture> connect() {
            if (connected) {
                return Optional.empty();
            }
            if (connectFuture == null) {
                connectFuture = bootstrap.connect();

                // 连接结束后，更改 channel 并删除本次的 connectFuture
                connectFuture.addListener(future -> {
                    if (future.isSuccess()) {
                        channel = connectFuture.channel();
                    } else {
                        channel = null;
                    }
                    connectFuture = null;
                });
            }
            return Optional.of(connectFuture);
        }

        @Override
        public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
            if (evt instanceof ConnectEvent) {
                connect();
                return;
            }
            if (evt instanceof ReconnectInterruptedEvent) {
                if (connectFuture != null) {
                    communicator().consoleWarn("net.reconnect.cancelled");
                    connectFuture.cancel(true);
                }
                return;
            }
            super.userEventTriggered(ctx, evt);
        }
    }
    @Getter(AccessLevel.NONE)
    private final ConnectHandler connectHandler = new ConnectHandler();

    /** 负责显示重连提示的 Handler */
    @ChannelHandler.Sharable
    private class ReconnectNoticeHandler extends ChannelInboundHandlerAdapter {
        @Override
        public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
            if (evt instanceof ReconnectDelayEvent) {
                onReconnectDelay((ReconnectDelayEvent) evt);
                return;
            }
            if (evt instanceof ReconnectFinallySucceedEvent) {
                onReconnectFinallySucceed((ReconnectFinallySucceedEvent) evt);
                return;
            }
            if (evt instanceof ReconnectFinallyFailedEvent) {
                onReconnectFinallyFailed((ReconnectFinallyFailedEvent) evt);
                return;
            }
            super.userEventTriggered(ctx, evt);
        }

        private void onReconnectFinallySucceed(ReconnectFinallySucceedEvent event) {
            communicator().consoleSuccess("net.reconnect.succeed");
        }

        private void onReconnectFinallyFailed(ReconnectFinallyFailedEvent event) {
            communicator().consoleError("net.reconnect.failed");
        }

        private void onReconnectDelay(ReconnectDelayEvent event) {
            final long delaySeconds = TimeUnit.MILLISECONDS.toSeconds(event.getDelayMillSeconds());
            communicator().consoleWarn("net.reconnect.delay", delaySeconds, event.getAttemptCount());
        }
    }
    @Getter(AccessLevel.NONE)
    private final ReconnectNoticeHandler reconnectNoticeHandler = new ReconnectNoticeHandler();

    /** 心跳发送器 */
    @ChannelHandler.Sharable
    public class HeartbeatHandler extends ChannelInboundHandlerAdapter {
        @Override
        public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
            if (evt instanceof IdleStateEvent) {
                communicator().consoleDebugString("write timeout, send heartbeat");
                sendHeartbeat();
            } else {
                super.userEventTriggered(ctx, evt);
            }
        }

        public void sendHeartbeat() {
            packetHandler.sign(BaseProtocol.HEART_BEAT);
        }
    }
    @Getter(AccessLevel.NONE)
    protected final HeartbeatHandler heartbeatHandler = new HeartbeatHandler();

    /** 异常掉线器 */
    @ChannelHandler.Sharable
    public class ExceptionHandler extends ChannelInboundHandlerAdapter {
        @Override
        public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
            ctx.channel().close();
            if (cause instanceof ConnectTimeoutException) {
                communicator().consoleWarn("net.state.exception.connect");
                return;
            }
            if (cause instanceof IOException) {
                communicator().consoleWarn("net.state.exception.io");
                return;
            }
            if (cause instanceof TimeoutException) {
                communicator().consoleWarn("net.state.exception.timeout");
                return;
            }
            communicator().consoleWarn("net.state.exception.unknown");
            communicator().consoleErrorString(Throwables.toStackTraces(cause));
        }
    }
    @Getter(AccessLevel.NONE)
    protected final ExceptionHandler exceptionHandler = new ExceptionHandler();

    /** 负责服务器身份验证的 Handler */
    @ChannelHandler.Sharable
    protected class VerifyHandler extends SimpleChannelInboundHandler<Packet> {
        protected final Box<Packet> msgBox = Box.empty();
        protected volatile boolean accepted = false;

        protected boolean shouldReconnect = true;

        @Override
        public void channelActive(ChannelHandlerContext ctx) throws Exception {
            // 启动验证线程和验证等待线程
            final Future<Object> future = executors.submit(() -> {
                Thread.sleep(plugin.getConnectionConfiguration().getResponseDelay());

                try {
                    reconnectStateHandler.disable();
                    verify(ctx);
                } finally {
                    if (shouldReconnect) {
                        reconnectStateHandler.enable();
                    }
                }

                return null;
            });

            executors.submit(() -> {
                try {
                    future.get(plugin.getConnectionConfiguration().getVerifyTimeout(), TimeUnit.MILLISECONDS);
                } catch (InterruptedException e) {
                    communicator().consoleInfo("net.verify.error.cancelled");
                } catch (ExecutionException e) {
                    final Throwable cause = e.getCause();

                    if (cause instanceof TimeoutException) {
                        communicator().consoleInfo("net.verify.error.timeout");
                        return;
                    }
                    if (cause instanceof ProtocolException) {
                        communicator().consoleError("net.verify.error.protocol", XMMCProtocol.VERSION);
                        return;
                    }

                    communicator().consoleError("net.verify.error.exception", Throwables.toStackTraces(cause));
                } catch (TimeoutException e) {
                    future.cancel(true);
                    communicator().consoleInfo("net.verify.error.timeout");
                } finally {
                    ctx.pipeline().remove(VerifyHandler.this);
                    if (accepted) {
                        ctx.pipeline().addLast(packetHandler);
                    } else {
                        disconnect();
                    }
                }
            });
            super.channelActive(ctx);
        }

        protected Optional<VerifyResponse> requestVerify(ChannelHandlerContext ctx) throws InterruptedException, TimeoutException {
            final ConnectionConfiguration configuration = plugin.getConnectionConfiguration();

            // 构造连接请求包
            final String passwordHash;
            try {
                passwordHash = PasswordHashUtil.createHash(configuration.getPassword());
            } catch (NoSuchAlgorithmException e) {
                communicator().consoleError("net.verify.error.algorithm");
                return Optional.empty();
            } catch (InvalidKeySpecException e) {
                communicator().consoleError("net.verify.error.pbkdf2");
                return Optional.empty();
            }
            final VerifyRequest verifyRequest = new VerifyRequest(passwordHash);

            // 构造请求包并发送
            ctx.writeAndFlush(new RequestPacket<>(XMMCProtocol.REQUEST_VERIFY, verifyRequest, 0));

            // 等待回应
            final VerifyResponse verifyResponse = nextResponse(XMMCProtocol.REQUEST_VERIFY, configuration.getResponseTimeout());

            return Optional.of(verifyResponse);
        }

        public void verify(ChannelHandlerContext ctx) throws Exception {
            final ConnectionConfiguration configuration = plugin.getConnectionConfiguration();

            // 构造验证请求并发送，得到回应
            final Optional<VerifyResponse> optionalVerifyResponse = requestVerify(ctx);
            if (!optionalVerifyResponse.isPresent()) {
                reconnectStateHandler.disable();
                return;
            }
            final VerifyResponse verifyResponse = optionalVerifyResponse.get();

            // 验证成功时回应 Normal
            // 没有人迎新服务器，也不认识该服务器，也回应 Normal
            if (verifyResponse instanceof VerifyResponse.Accepted) {
                accepted = true;
                name = ((VerifyResponse.Accepted) verifyResponse).getName();
                communicator().consoleSuccess("net.verify.accepted", name);
                return;
            }
            if (verifyResponse instanceof VerifyResponse.Conflict) {
                communicator().consoleWarn("net.verify.conflict");
                shouldReconnect = false;
                return;
            }
            if (verifyResponse instanceof VerifyResponse.Cancelled) {
                communicator().consoleWarn("net.verify.cancelled");
                shouldReconnect = false;
                return;
            }
            if (verifyResponse instanceof VerifyResponse.Denied) {
                communicator().consoleWarn("net.verify.denied");
                shouldReconnect = false;
                return;
            }

            // 如果不认识该服务器
            // 但是此时有人迎接新服务器
            // 则回应 Confirm，显示服务器验证码
            if (verifyResponse instanceof VerifyResponse.Confirm) {
                final VerifyResponse.Confirm confirm = (VerifyResponse.Confirm) verifyResponse;
                if (verifyResponse instanceof VerifyResponse.Confirm.Busy) {
                    communicator().consoleWarn("net.verify.confirm.busy");
                    shouldReconnect = false;
                    return;
                }
                final VerifyResponse.Confirm.Operated operated = (VerifyResponse.Confirm.Operated) confirm;

                final String verifyCode = operated.getVerifyCode();
                final long timeout = operated.getTimeout();

                communicator().consoleInfo("net.verify.confirm.message", Times.toTimeLength(timeout), verifyCode);

                // 等待确认消息
                final Packet packet = nextPacket(x -> x instanceof TypePacket
                        && Objects.equals(((TypePacket<?>) x).getPacketType(), XMMCProtocol.REQUEST_CONFIRM), timeout);
                XMMCProtocol.checkProtocol(packet instanceof RequestPacket);

                @SuppressWarnings("all")
                final RequestPacket<ConfirmRequest, Boolean> confirmRequestPacket = (RequestPacket<ConfirmRequest, Boolean>) packet;
                final ConfirmRequest confirmRequest = confirmRequestPacket.getData();

                // 如果允许连接，则连接成功，否则失败
                if (confirmRequest instanceof ConfirmRequest.Denied) {
                    communicator().consoleSuccess("net.verify.confirm.denied");
                } else if (confirmRequest instanceof ConfirmRequest.Accepted) {
                    final ConfirmRequest.Accepted accepted = (ConfirmRequest.Accepted) confirmRequest;
                    this.accepted = true;
                    communicator().consoleSuccess("net.verify.confirm.accepted", accepted.getName());
                    configuration.setPassword(accepted.getPassword());
                    configuration.save();
                }

                // 稍后再回复
                final boolean finalAccepted = this.accepted;
                executors.schedule(() -> ctx.writeAndFlush(
                        new ResponsePacket<>(finalAccepted, confirmRequestPacket.getPacketCode(), 0)), configuration.getResponseDelay(), TimeUnit.MILLISECONDS);
            }
        }

        @Override
        protected void channelRead0(ChannelHandlerContext channelHandlerContext, Packet packet) throws Exception {
            msgBox.set(packet);
        }

        @SuppressWarnings("all")
        public Packet nextPacket(Predicate<Packet> assertion, long timeout) throws InterruptedException, TimeoutException, ProtocolException {
            final Container<Packet> nextValue = msgBox.nextValue(timeout);
            if (nextValue.isEmpty()) {
                throw new TimeoutException();
            }
            final Packet packet = nextValue.get();

            if (!assertion.test(packet)) {
                XMMCProtocol.checkProtocol(false);
            }

            return packet;
        }

        @SuppressWarnings("all")
        public <T> T nextResponse(ResponsiblePacketType<T> packetType, long timeout) throws InterruptedException, TimeoutException {
            final Packet packet = msgBox.nextValue(timeout).orElseThrow(TimeoutException::new);
            if (!(packet instanceof ResponsePacket)) {
                XMMCProtocol.checkProtocol(false);
            }

            return ((ResponsePacket<T>) packet).getData();
        }
    }

    public Client(XMMCBukkitPlugin plugin) {
        super(plugin);

        setupConfiguration();

        packetHandler.setDefaultMessageListener(packet -> {
            communicator().consoleDebugString("no type packet: " + packet);
        });

        bootstrap.channel(NioSocketChannel.class)
                .handler(new ChannelInitializer<NioSocketChannel>() {
                    @Override
                    protected void initChannel(NioSocketChannel channel) throws Exception {
                        Client.this.channel = channel;
                        final ChannelPipeline pipeline = channel.pipeline();

                        // 长度域解包器
                        pipeline.addLast(new LengthFieldBasedFrameDecoder(Integer.MAX_VALUE, 0, 4, 0, 4));
                        pipeline.addLast(new LengthFieldPrepender(4));

                        // 编码器和解码器
                        pipeline.addLast(new StringDecoder(StandardCharsets.UTF_8));
                        pipeline.addLast(new StringEncoder(StandardCharsets.UTF_8));

                        // Json 反序列化器
                        pipeline.addLast(new JsonPacketCodec(XMMCProtocol.getInstance()));
                        pipeline.addLast(new DebugDuplexHandler("packet", communicator().toLogger()));

                        // 心跳检测器
                        final ConnectionConfiguration connection = plugin.getConnectionConfiguration();
                        pipeline.addLast(new IdleStateHandler(0, connection.getHeartbeatTimeout(), 0, TimeUnit.MILLISECONDS));
                        pipeline.addLast(heartbeatHandler);

                        // 验证器
                        pipeline.addLast(new VerifyHandler());

                        // 其他逻辑
                        pipeline.addLast(reconnectStateHandler);
                        pipeline.addLast(reconnectNoticeHandler);
                        pipeline.addLast(exceptionHandler);
                        pipeline.addLast(connectHandler);
                    }
                });
    }

    public void setupConfiguration() {
        final ConnectionConfiguration connection = plugin.getConnectionConfiguration();

        bootstrap.remoteAddress(connection.getHost(), connection.getPort());

        packetHandler.setResponseDelay(connection.getResponseDelay());
        packetHandler.setResponseTimeout(connection.getResponseTimeout());

        reconnectStateHandler.setBaseReconnectDelay(connection.getBaseReconnectDelay());
        reconnectStateHandler.setDeltaReconnectDelay(connection.getDeltaReconnectDelay());
        reconnectStateHandler.setMaxAttempts(connection.getMaxAttemptCount());

        if (Objects.isNull(executors)) {
            executors = new NioEventLoopGroup(connection.getThreadCount());
            bootstrap.group(executors);
        }
    }

    public Optional<ChannelFuture> connect() {
        reconnectStateHandler.reset();
        return connectHandler.connect();
    }

    public boolean isConnected() {
        return connectHandler.connected;
    }

    public Optional<ChannelFuture> disconnectManually() {
        if (!isConnected()) {
            return Optional.empty();
        }

        reconnectStateHandler.disable(true);
        return Optional.of(channel.disconnect());
    }

    public Optional<ChannelFuture> disconnect() {
        if (!isConnected()) {
            return Optional.empty();
        }

        return Optional.of(channel.disconnect());
    }
}