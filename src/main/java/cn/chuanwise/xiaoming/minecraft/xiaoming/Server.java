package cn.chuanwise.xiaoming.minecraft.xiaoming;

import cn.chuanwise.net.ProtocolException;
import cn.chuanwise.net.packet.*;
import cn.chuanwise.toolkit.box.Box;
import cn.chuanwise.toolkit.container.Container;
import cn.chuanwise.util.ConditionUtil;
import cn.chuanwise.util.ObjectUtil;
import cn.chuanwise.xiaoming.minecraft.protocol.ConfirmRequest;
import cn.chuanwise.xiaoming.minecraft.util.PasswordHashUtil;
import cn.chuanwise.xiaoming.minecraft.protocol.VerifyRequest;
import cn.chuanwise.xiaoming.minecraft.protocol.VerifyResponse;
import cn.chuanwise.xiaoming.minecraft.protocol.XMMCProtocol;
import cn.chuanwise.xiaoming.minecraft.xiaoming.configuration.Configuration;
import cn.chuanwise.xiaoming.minecraft.xiaoming.configuration.ServerInfo;
import cn.chuanwise.xiaoming.minecraft.xiaoming.interactors.VerifyInteractors;
import cn.chuanwise.xiaoming.object.PluginObjectImpl;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.Channel;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import io.netty.handler.codec.LengthFieldPrepender;
import io.netty.handler.codec.string.StringDecoder;
import io.netty.handler.codec.string.StringEncoder;
import io.netty.util.concurrent.Future;
import lombok.Getter;

import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Predicate;

@Getter
public class Server extends PluginObjectImpl<Plugin> {
    protected final ServerBootstrap serverBootstrap = new ServerBootstrap();

    protected NioEventLoopGroup executors;

    protected Channel channel;
    protected List<OnlineClient> onlineClients = new CopyOnWriteArrayList<>();

    public List<OnlineClient> getOnlineClients() {
        return Collections.unmodifiableList(onlineClients);
    }

    @ChannelHandler.Sharable
    private class VerifyHandler extends SimpleChannelInboundHandler<Packet> {
        protected final Box<Packet> msgBox = Box.empty();
        protected volatile boolean accepted;

        @Override
        public void channelActive(ChannelHandlerContext ctx) throws Exception {
            final Configuration configuration = plugin.getConfiguration();
            final Configuration.Connection connection = configuration.getConnection();

            final Future<Object> future = executors.submit(() -> {
                verify(ctx);
                return null;
            });

            executors.submit(() -> {
                try {
                    future.get(connection.getVerifyTimeout(), TimeUnit.MILLISECONDS);

                    if (!accepted) {
                        ctx.channel().close();
                    }
                } catch (InterruptedException e) {
                    plugin.getLogger().warn("等待验证被打断，强制断开客户端连接");
                    ctx.channel().close();
                } catch (ExecutionException e) {
                    final Throwable cause = e.getCause();
                    plugin.getLogger().error("验证时出现异常", cause);
                    ctx.channel().close();
                } catch (TimeoutException e) {
                    plugin.getLogger().error("验证超时");
                    ctx.channel().close();
                } finally {
                    ctx.pipeline().remove(VerifyHandler.this);
                }
            });
        }

        protected void verify(ChannelHandlerContext ctx) throws Exception {
            // 服务器端会发送 XMMCProtocol.REQUEST_VERIFY 类型的包
            final Configuration configuration = plugin.configuration;
            final Configuration.Connection connection = configuration.getConnection();

            final Packet packet = nextPacket(x -> x instanceof RequestPacket
                    && Objects.equals(((RequestPacket<?, ?>) x).getPacketType(), XMMCProtocol.REQUEST_VERIFY), connection.getResponseTimeout());
            XMMCProtocol.checkProtocol(packet instanceof RequestPacket);

            @SuppressWarnings("unchecked") final RequestPacket<VerifyRequest, VerifyResponse> verifyRequestPacket = (RequestPacket<VerifyRequest, VerifyResponse>) packet;

            final VerifyRequest verifyRequest = verifyRequestPacket.getData();
            final String passwordHash = verifyRequest.getPasswordHash();

            // 寻找使用此密码的服务器
            ServerInfo serverInfo = null;
            for (ServerInfo info : configuration.getServers().values()) {
                if (PasswordHashUtil.validatePassword(info.getPassword(), passwordHash)) {
                    serverInfo = info;
                    break;
                }
            }

            // 验证成功
            if (Objects.nonNull(serverInfo)) {
                accepted = true;
                final VerifyResponse.Accepted verifyResponse = new VerifyResponse.Accepted(serverInfo.getName());
                ctx.writeAndFlush(new ResponsePacket<>(verifyResponse, 0, 0));
                onlineClients.add(new OnlineClient(Server.this, ctx.channel(), serverInfo));

                plugin.getLogger().info("服务器 " + serverInfo.getName() + " 连接到小明");
                return;
            }

            final VerifyInteractors verifyInteractors = plugin.getVerifyInteractors();

            // 检查目前是否允许新服务器接入
            // 如果不允许就返回错误
            if (!verifyInteractors.isAllowStrangeServerConnect()) {
                final VerifyResponse.Denied verifyResponse = new VerifyResponse.Denied();
                ctx.writeAndFlush(new ResponsePacket<>(verifyResponse, 0, 0));

                plugin.getLogger().info("陌生服务器连接到小明，但因为尚未开启迎新模式，故被拒绝连接");
                return;
            }
            final Optional<VerifyInteractors.MeetingContext> optionalMeetingContext = verifyInteractors.onMeetingActive();

            // 如果此时正在忙碌
            // 回复忙碌消息
            if (optionalMeetingContext.isEmpty()) {
                plugin.getLogger().info("新服务器连接到小明，但迎新 QQ 忙碌，故拒绝了连接");
                ctx.writeAndFlush(new ResponsePacket<>(new VerifyResponse.Confirm.Busy(), 0, 0));
                return;
            }
            final VerifyInteractors.MeetingContext meetingContext = optionalMeetingContext.get();

            // 回复验证码等信息
            final long verifyTimeout = connection.getVerifyTimeout();
            final String verifyCode = meetingContext.getVerifyCode();
            final VerifyResponse.Confirm verifyResponse = new VerifyResponse.Confirm.Operated(verifyCode, verifyTimeout);
            ctx.writeAndFlush(new ResponsePacket<>(verifyResponse, 0, 0));
            plugin.getLogger().info("新服务器连接到小明，验证码：" + verifyCode + "，正在等待迎新人回应");

            // 启动线程等待结果
            final Object condition = meetingContext.getCondition();
            try {
                if (ObjectUtil.wait(condition, verifyTimeout)) {
                    // 如果等到了结果
                    ConditionUtil.checkState(meetingContext.isHandled(), "internal error");
                    accepted = meetingContext.isAccepted();
                    plugin.getLogger().info("迎新人员" + (accepted ? "批准" : "拒绝") + "了验证码为 " + verifyCode + " 的服务器的连接");

                    if (accepted) {
                        serverInfo = meetingContext.getServerInfo();
                        onlineClients.add(new OnlineClient(Server.this, ctx.channel(), serverInfo));
                    }
                } else {
                    plugin.getLogger().info("迎新超时，已拒绝服务器连接");
                    meetingContext.deny(xiaomingBot.getCode());
                }
            } catch (InterruptedException exception) {
                plugin.getLogger().error("等待迎新结果被打断");
                meetingContext.deny(xiaomingBot.getCode());
            } catch (Exception exception) {
                plugin.getLogger().error("等待迎新结果时出现异常", exception);
                meetingContext.deny(xiaomingBot.getCode());
            } finally {
                if (accepted) {
                    final ConfirmRequest.Accepted accepted = new ConfirmRequest.Accepted(serverInfo.getName(), serverInfo.getPassword());
                    ctx.writeAndFlush(new RequestPacket<>(XMMCProtocol.REQUEST_CONFIRM, accepted, 0));
                } else {
                    ctx.writeAndFlush(new RequestPacket<>(XMMCProtocol.REQUEST_CONFIRM, new ConfirmRequest.Denied(), 0));
                }
            }
        }

        @Override
        protected void channelRead0(ChannelHandlerContext context, Packet packet) throws Exception {
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

    public Server(Plugin plugin) {
        setPlugin(plugin);

        serverBootstrap.channel(NioServerSocketChannel.class)
                .option(ChannelOption.SO_REUSEADDR, true)
                .childHandler(new ChannelInitializer<>() {
                    @Override
                    protected void initChannel(Channel channel) throws Exception {
                        final ChannelPipeline pipeline = channel.pipeline();

                        pipeline.addLast(new LengthFieldBasedFrameDecoder(Integer.MAX_VALUE, 0, 4, 0, 4));
                        pipeline.addLast(new LengthFieldPrepender(4));

                        pipeline.addLast(new StringDecoder(StandardCharsets.UTF_8));
                        pipeline.addLast(new StringEncoder(StandardCharsets.UTF_8));

                        pipeline.addLast(new JsonPacketCodeC(XMMCProtocol.getInstance()));
                        pipeline.addLast(new VerifyHandler());
                    }

                    @Override
                    public void channelInactive(ChannelHandlerContext context) throws Exception {
                        onlineClients.removeIf(x -> x.getChannel() == context.channel());
                    }
                });

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            if (Objects.isNull(executors)) {
                return;
            }

            if (!executors.isTerminated()) {
                executors.shutdownGracefully();
            }
        }));
    }

    public void setupConfiguration() {
        final Configuration configuration = plugin.getConfiguration();
        final Configuration.Connection connection = configuration.getConnection();

        if (Objects.isNull(executors)) {
            executors = new NioEventLoopGroup(connection.getThreadCount());
            serverBootstrap.group(executors);
        }

        serverBootstrap.localAddress(connection.getPort());
    }

    public boolean isBound() {
        return Objects.nonNull(channel) && channel.isActive();
    }

    public Optional<ChannelFuture> bind() {
        if (isBound()) {
            return Optional.empty();
        }

        setupConfiguration();

        final ChannelFuture channelFuture = serverBootstrap.bind();
        channelFuture.addListener(future -> {
            if (future.isSuccess()) {
                this.channel = channelFuture.channel();
            }
        });
        channelFuture.channel().closeFuture().addListener(future -> {
            onlineClients.clear();
            this.channel = null;
        });
        return Optional.of(channelFuture);
    }

    public Optional<ChannelFuture> unbind() {
        if (!isBound()) {
            return Optional.empty();
        }

        final ChannelFuture closeFuture = channel.close();

        return Optional.of(closeFuture);
    }
}
