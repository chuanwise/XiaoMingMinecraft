package cn.chuanwise.xiaoming.minecraft.bukkit;

import cn.chuanwise.mclib.bukkit.BukkitPluginObject;
import cn.chuanwise.mclib.net.bukkit.BukkitLocalContact;
import cn.chuanwise.net.DebugDuplexHandler;
import cn.chuanwise.net.ProtocolException;
import cn.chuanwise.net.ReconnectHandler;
import cn.chuanwise.net.packet.*;
import cn.chuanwise.toolkit.box.Box;
import cn.chuanwise.toolkit.container.Container;
import cn.chuanwise.util.ThrowableUtil;
import cn.chuanwise.util.TimeUtil;
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
import io.netty.util.concurrent.Future;
import lombok.AccessLevel;
import lombok.Getter;

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

    protected NettyPacketService packetService = new NettyPacketService();

    /** 本地会话处理器 */
    protected final BukkitLocalContact<Plugin> contact = new BukkitLocalContact<>(packetService, log(), plugin);

    /** 重连工具 */
    protected final ReconnectHandler reconnectHandler = new ReconnectHandler(bootstrap);
    protected NioEventLoopGroup executors;

    @Getter(AccessLevel.NONE)
    protected Channel channel;

    @Getter(AccessLevel.NONE)
    private final Object verifyCondition = new Object();

    protected volatile String name;

    /** 负责服务器身份验证的 Handler */
    @ChannelHandler.Sharable
    protected class VerifyHandler extends SimpleChannelInboundHandler<Packet> {
        protected final Box<Packet> msgBox = Box.empty();
        protected volatile boolean accepted = false;

        @Override
        public void channelActive(ChannelHandlerContext ctx) throws Exception {
            // 启动验证线程和验证等待线程
            final Future<Object> future = executors.submit(() -> {
                Thread.sleep(plugin.configuration.connection.responseDelay);
                verify(ctx);
                return null;
            });

            log().debug("channel active, and submit task waiting for verifying");
            executors.submit(() -> {
                try {
                    log().debug("waiting for verifying...");
                    future.get(plugin.configuration.connection.verifyTimeout, TimeUnit.MILLISECONDS);
                    log().debug("verify result: accepted = " + accepted);
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
                    if (!accepted) {
                        disconnect();
                    } else {
                        ctx.pipeline().addLast(packetService);
                    }
                }
            });
        }

        protected Optional<VerifyResponse> requestVerify(ChannelHandlerContext ctx) throws InterruptedException, TimeoutException {
            final Configuration.Connection connection = plugin.configuration.connection;

            // 构造连接请求包
            final String passwordHash;
            try {
                passwordHash = PasswordHashUtil.createHash(connection.password);
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
            log().debug("verify request: pwd = " + connection.password + ", pbkdf2 hash = " + passwordHash);

            // 等待回应
            final VerifyResponse verifyResponse = nextResponse(XMMCProtocol.REQUEST_VERIFY, connection.responseTimeout);
            log().debug("response for connect request: " + verifyResponse);

            return Optional.of(verifyResponse);
        }

        public void verify(ChannelHandlerContext ctx) throws Exception {
            final Configuration configuration = plugin.getConfiguration();
            final Configuration.Connection connection = configuration.connection;

            // 构造验证请求并发送，得到回应
            final Optional<VerifyResponse> optionalVerifyResponse = requestVerify(ctx);
            if (optionalVerifyResponse.isEmpty()) {
                reconnectHandler.setStopToReconnect(true);
                return;
            }
            final VerifyResponse verifyResponse = optionalVerifyResponse.get();

            // 验证成功时回应 Normal
            // 没有人迎新服务器，也不认识该服务器，也回应 Normal
            if (verifyResponse instanceof VerifyResponse.Accepted) {
                accepted = true;
                name = ((VerifyResponse.Accepted) verifyResponse).getName();
                communicator().consoleWarn("net.verify.accepted", name);
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
                    configuration.connection.setPassword(accepted.getPassword());
                    configuration.save();
                }

                // 稍后再回复
                final boolean finalAccepted = this.accepted;
                executors.schedule(() -> ctx.writeAndFlush(
                        new ResponsePacket<>(finalAccepted, confirmRequestPacket.getPacketCode(), 0)), connection.responseDelay, TimeUnit.MILLISECONDS);
            }
        }

        @Override
        protected void channelRead0(ChannelHandlerContext channelHandlerContext, Packet packet) throws Exception {
            msgBox.set(packet);
        }

        @SuppressWarnings("unchecked")
        public Packet nextPacket(Predicate<Packet> assertion, long timeout) throws InterruptedException, TimeoutException, ProtocolException {
            final Container<Packet> nextValue = msgBox.nextValue(timeout);
            if (nextValue.isEmpty()) {
                throw new TimeoutException();
            }
            final Packet packet = nextValue.get();

            if (!assertion.test(packet)) {
                throw new ProtocolException();
            }

            return packet;
        }

        @SuppressWarnings("unchecked")
        public <T> T nextResponse(ResponsiblePacketType<T> packetType, long timeout) throws InterruptedException, TimeoutException {
            final Packet packet = msgBox.nextValue(timeout).orElseThrow(TimeoutException::new);
            if (!(packet instanceof ResponsePacket)) {
                throw new ProtocolException();
            }

            return ((ResponsePacket<T>) packet).getData();
        }
    }

    public Client(Plugin plugin) {
        super(plugin);

        setupConfiguration();

        reconnectHandler.setOnReconnectSuccessfully(() -> {
            consoleSender().sendMessage(language().formatSuccessNode("net.reconnect.succeed"));
        });
        reconnectHandler.setOnOutOfTotalReconnectBound(() -> {
            consoleSender().sendMessage(language().formatWarnNode("net.reconnect.outOfBound.total", reconnectHandler.getMaxTotalReconnectFailCount()));
        });
        reconnectHandler.setOnOutOfRecentReconnectBound(() -> {
            consoleSender().sendMessage(language().formatWarnNode("net.reconnect.outOfBound.recent", reconnectHandler.getMaxRecentReconnectFailCount()));
        });
        reconnectHandler.setOnReconnectException(throwable -> { });
        reconnectHandler.setOnBeforeNextReconnectSleep(() -> {
            consoleSender().sendMessage(language().formatWarnNode("net.reconnect.beforeSleep", TimeUnit.MILLISECONDS.toSeconds(reconnectHandler.getNextReconnectDelay()), reconnectHandler.getRecentReconnectFailCount() + 1));
        });

        bootstrap.group(executors)
                .channel(NioSocketChannel.class)
                .handler(new ChannelInitializer<NioSocketChannel>() {
                    @Override
                    protected void initChannel(NioSocketChannel channel) throws Exception {
                        Client.this.channel = channel;
                        final ChannelPipeline pipeline = channel.pipeline();

                        pipeline.addLast(new LengthFieldBasedFrameDecoder(Integer.MAX_VALUE, 0, 4, 0, 4));
                        pipeline.addLast(new LengthFieldPrepender(4));

                        pipeline.addLast(new StringDecoder(StandardCharsets.UTF_8));
                        pipeline.addLast(new StringEncoder(StandardCharsets.UTF_8));

                        pipeline.addLast(new DebugDuplexHandler(communicator().toLogger()));
                        pipeline.addLast(new JsonPacketCodeC(XMMCProtocol.getInstance()));

                        pipeline.addLast(new VerifyHandler());
                        pipeline.addLast(reconnectHandler);
                    }
                });

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

    public void setupConfiguration() {
        final Configuration.Connection connection = plugin.configuration.getConnection();

        bootstrap.remoteAddress(connection.getHost(), connection.getPort());

        packetService.setResponseDelay(connection.getResponseDelay());
        packetService.setResponseTimeout(connection.getResponseTimeout());

        reconnectHandler.setMaxRecentReconnectFailCount(connection.getMaxRecentReconnectFailCount());
        reconnectHandler.setMaxTotalReconnectFailCount(connection.getMaxTotalReconnectFailCount());
        reconnectHandler.setBaseReconnectDelay(connection.getBaseReconnectDelay());
        reconnectHandler.setDeltaReconnectDelay(connection.getDeltaReconnectDelay());

        if (Objects.isNull(executors)) {
            executors = new NioEventLoopGroup(connection.threadCount);
        }
    }

    public Optional<ChannelFuture> connect() {
        if (isConnected()) {
            return Optional.empty();
        }

        reconnectHandler.setRecentReconnectFailCount(0);
        reconnectHandler.setStopToReconnect(false);

        setupConfiguration();

        final ChannelFuture channelFuture = bootstrap.connect();
        this.channel = channelFuture.channel();

        return Optional.of(channelFuture);
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