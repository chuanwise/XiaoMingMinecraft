package cn.chuanwise.xiaoming.minecraft.bukkit.net;

import cn.chuanwise.api.Logger;
import cn.chuanwise.mclib.bukkit.BukkitPluginObject;
import cn.chuanwise.mclib.net.bukkit.BukkitLocalContact;
import cn.chuanwise.net.netty.*;
import cn.chuanwise.net.netty.packet.JsonPacketCodeC;
import cn.chuanwise.net.netty.packet.PacketHandler;
import cn.chuanwise.net.packet.*;
import cn.chuanwise.toolkit.box.Box;
import cn.chuanwise.toolkit.container.Container;
import cn.chuanwise.util.ThrowableUtil;
import cn.chuanwise.util.TimeUtil;
import cn.chuanwise.xiaoming.minecraft.bukkit.Configuration;
import cn.chuanwise.xiaoming.minecraft.bukkit.Plugin;
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
public class Client extends BukkitPluginObject<Plugin> {
    /** 连接的引导器 */
    protected final Bootstrap bootstrap = new Bootstrap();

    protected PacketHandler packetHandler = new PacketHandler();

    /** 本地会话处理器 */
    protected final BukkitLocalContact<Plugin> localContact = new BukkitLocalContact<>(packetHandler, communicator().toLogger(), plugin);
    protected final XMMCClientContact clientContact = new XMMCClientContact(plugin, packetHandler);

    /** 重连工具 */
    protected final ReconnectHandler reconnectHandler = new ReconnectHandler(bootstrap);
    protected final ListenerHandler listenerHandler = new ListenerHandler();
    protected NioEventLoopGroup executors;

    @Getter(AccessLevel.NONE)
    protected Channel channel;

    @Getter(AccessLevel.NONE)
    private final Object verifyCondition = new Object();

    protected volatile String name;

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
            communicator().consoleErrorString(ThrowableUtil.toStackTraces(cause));
        }
    }
    protected final ExceptionHandler exceptionHandler = new ExceptionHandler();

    /** 负责服务器身份验证的 Handler */
    @ChannelHandler.Sharable
    protected class VerifyHandler extends SimpleChannelInboundHandler<Packet> {
        protected final Box<Packet> msgBox = Box.empty();
        protected volatile boolean accepted = false;

        @Override
        public void channelActive(ChannelHandlerContext ctx) throws Exception {
            // 启动验证线程和验证等待线程
            final Future<Object> future = executors.submit(() -> {
                Thread.sleep(plugin.getConfiguration().getConnection().getResponseDelay());
                verify(ctx);
                return null;
            });

            logger().debug("channel active, and submit task waiting for verifying");
            executors.submit(() -> {
                try {
                    logger().debug("waiting for verifying...");
                    future.get(plugin.getConfiguration().getConnection().getVerifyTimeout(), TimeUnit.MILLISECONDS);
                    logger().debug("verify result: accepted = " + accepted);
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

                    communicator().consoleError("net.verify.error.exception", ThrowableUtil.toStackTraces(cause));
                } catch (TimeoutException e) {
                    future.cancel(true);
                    communicator().consoleInfo("net.verify.error.timeout");
                } finally {
                    ctx.pipeline().remove(VerifyHandler.this);
                    if (accepted) {
                        ctx.pipeline().addLast(packetHandler);
                        ctx.pipeline().addLast(heartbeatHandler);
                        ctx.pipeline().addLast(exceptionHandler);
                    } else {
                        disconnect();
                    }
                }
            });
        }

        protected Optional<VerifyResponse> requestVerify(ChannelHandlerContext ctx) throws InterruptedException, TimeoutException {
            final Configuration.Connection connection = plugin.getConfiguration().getConnection();

            // 构造连接请求包
            final String passwordHash;
            try {
                passwordHash = PasswordHashUtil.createHash(connection.getPassword());
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
            logger().debug("verify request: pwd = " + connection.getPassword() + ", pbkdf2 hash = " + passwordHash);

            // 等待回应
            final VerifyResponse verifyResponse = nextResponse(XMMCProtocol.REQUEST_VERIFY, connection.getResponseTimeout());
            logger().debug("response for connect request: " + verifyResponse);

            return Optional.of(verifyResponse);
        }

        public void verify(ChannelHandlerContext ctx) throws Exception {
            final Configuration configuration = plugin.getConfiguration();
            final Configuration.Connection connection = configuration.getConnection();

            // 构造验证请求并发送，得到回应
            final Optional<VerifyResponse> optionalVerifyResponse = requestVerify(ctx);
            if (!optionalVerifyResponse.isPresent()) {
                reconnectHandler.setStopToReconnect(true);
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
                reconnectHandler.setStopToReconnect(true);
                return;
            }
            if (verifyResponse instanceof VerifyResponse.Denied) {
                communicator().consoleWarn("net.verify.denied");
                reconnectHandler.setStopToReconnect(true);
                return;
            }

            // 如果不认识该服务器
            // 但是此时有人迎接新服务器
            // 则回应 Confirm，显示服务器验证码
            if (verifyResponse instanceof VerifyResponse.Confirm) {
                final VerifyResponse.Confirm confirm = (VerifyResponse.Confirm) verifyResponse;
                if (verifyResponse instanceof VerifyResponse.Confirm.Busy) {
                    communicator().consoleWarn("net.verify.confirm.busy");
                    reconnectHandler.setStopToReconnect(true);
                    return;
                }
                final VerifyResponse.Confirm.Operated operated = (VerifyResponse.Confirm.Operated) confirm;

                final String verifyCode = operated.getVerifyCode();
                final long timeout = operated.getTimeout();

                communicator().consoleInfo("net.verify.confirm.message", TimeUtil.toTimeLength(timeout), verifyCode);

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
                    configuration.getConnection().setPassword(accepted.getPassword());
                    configuration.save();
                }

                // 稍后再回复
                final boolean finalAccepted = this.accepted;
                executors.schedule(() -> ctx.writeAndFlush(
                        new ResponsePacket<>(finalAccepted, confirmRequestPacket.getPacketCode(), 0)), connection.getResponseDelay(), TimeUnit.MILLISECONDS);
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

    public Client(Plugin plugin) {
        super(plugin);

        setupConfiguration();

        packetHandler.setDefaultMessageListener(packet -> {
            communicator().consoleDebugString("no type packet: " + packet);
        });

        // 更新 channel
        listenerHandler.getAddListeners().add(HandlerListener.repetitive(context -> {
            Client.this.channel = context.channel();
        }));

        reconnectHandler.setOnReconnectSuccessfully(() -> {
            consoleSender().sendMessage(language().formatSuccessNode("net.reconnect.succeed"));
        });
        reconnectHandler.setOnOutOfTotalReconnectBound(() -> {
            consoleSender().sendMessage(language().formatWarnNode("net.reconnect.outOfBound.total", reconnectHandler.getMaxTotalReconnectFailCount()));
        });
        reconnectHandler.setOnOutOfRecentReconnectBound(() -> {
            consoleSender().sendMessage(language().formatWarnNode("net.reconnect.outOfBound.recent", reconnectHandler.getMaxRecentReconnectFailCount()));
        });
        reconnectHandler.setOnReconnectException(throwable -> {});
        reconnectHandler.setOnBeforeNextReconnectSleep(() -> {
            consoleSender().sendMessage(language().formatWarnNode("net.reconnect.beforeSleep", TimeUnit.MILLISECONDS.toSeconds(reconnectHandler.getNextReconnectDelay()), reconnectHandler.getRecentReconnectFailCount() + 1));
        });

        bootstrap.channel(NioSocketChannel.class)
                .handler(new ChannelInitializer<NioSocketChannel>() {
                    @Override
                    protected void initChannel(NioSocketChannel channel) throws Exception {
                        Client.this.channel = channel;
                        final ChannelPipeline pipeline = channel.pipeline();

                        final Logger logger = communicator().toLogger();
                        final Configuration.Connection connection = plugin.getConfiguration().getConnection();
                        pipeline.addLast(new IdleStateHandler(0, connection.getIdlePeriod(), 0, TimeUnit.MILLISECONDS));

                        pipeline.addLast(new LengthFieldBasedFrameDecoder(Integer.MAX_VALUE, 0, 4, 0, 4));
                        pipeline.addLast(new LengthFieldPrepender(4));

                        pipeline.addLast(new StringDecoder(StandardCharsets.UTF_8));
                        pipeline.addLast(new StringEncoder(StandardCharsets.UTF_8));

                        pipeline.addLast(new JsonPacketCodeC(XMMCProtocol.getInstance()));
                        pipeline.addLast(new DebugDuplexHandler("packet", logger));

                        pipeline.addLast(listenerHandler);

                        pipeline.addLast(new VerifyHandler());
                        pipeline.addLast(reconnectHandler);
                    }
                });
    }

    public void setupConfiguration() {
        final Configuration.Connection connection = plugin.getConfiguration().getConnection();

        bootstrap.remoteAddress(connection.getHost(), connection.getPort());

        packetHandler.setResponseDelay(connection.getResponseDelay());
        packetHandler.setResponseTimeout(connection.getResponseTimeout());

        reconnectHandler.setMaxRecentReconnectFailCount(connection.getMaxRecentReconnectFailCount());
        reconnectHandler.setMaxTotalReconnectFailCount(connection.getMaxTotalReconnectFailCount());
        reconnectHandler.setBaseReconnectDelay(connection.getBaseReconnectDelay());
        reconnectHandler.setDeltaReconnectDelay(connection.getDeltaReconnectDelay());

        if (Objects.isNull(executors)) {
            executors = new NioEventLoopGroup(connection.getThreadCount());
            bootstrap.group(executors);
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                if (Objects.isNull(executors)) {
                    return;
                }
                if (executors.isTerminated()) {
                    return;
                }
                executors.shutdownGracefully();
            }));
        }
    }

    @Getter(AccessLevel.NONE)
    private volatile ChannelFuture recentConnectFuture;

    public Optional<ChannelFuture> connect() {
        if (isConnected()) {
            return Optional.empty();
        }

        if (Objects.nonNull(recentConnectFuture)) {
            return Optional.of(recentConnectFuture);
        }

        final ChannelFuture reconnectFuture = reconnectHandler.getReconnectFuture();
        if (Objects.nonNull(reconnectFuture)) {
            return Optional.of(reconnectFuture);
        }

        reconnectHandler.setRecentReconnectFailCount(0);
        reconnectHandler.setStopToReconnect(false);
        reconnectHandler.stopReconnecting();

        setupConfiguration();

        final ChannelFuture connectFuture = bootstrap.connect();
        recentConnectFuture = connectFuture;
        this.channel = connectFuture.channel();

        connectFuture.addListener(x -> {
            recentConnectFuture = null;
        });
        return Optional.of(connectFuture);
    }

    public boolean isConnected() {
        return Objects.nonNull(channel) && channel.isActive();
    }

    public Optional<ChannelFuture> disconnectManually() {
        if (!isConnected()) {
            return Optional.empty();
        }

        reconnectHandler.stopReconnecting();
        reconnectHandler.setStopToReconnect(true);

        return Optional.of(channel.disconnect());
    }

    public Optional<ChannelFuture> disconnect() {
        if (!isConnected()) {
            return Optional.empty();
        }

        return Optional.of(channel.disconnect());
    }
}