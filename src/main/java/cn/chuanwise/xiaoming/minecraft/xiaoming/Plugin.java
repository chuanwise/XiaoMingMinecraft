package cn.chuanwise.xiaoming.minecraft.xiaoming;

import cn.chuanwise.toolkit.container.Container;
import cn.chuanwise.util.CollectionUtil;
import cn.chuanwise.util.ConditionUtil;
import cn.chuanwise.xiaoming.minecraft.xiaoming.configuration.Configuration;
import cn.chuanwise.xiaoming.minecraft.xiaoming.configuration.ServerInfo;
import cn.chuanwise.xiaoming.minecraft.xiaoming.interactors.ConnectionInteractors;
import cn.chuanwise.xiaoming.minecraft.xiaoming.interactors.StateInteractors;
import cn.chuanwise.xiaoming.minecraft.xiaoming.interactors.VerifyInteractors;
import cn.chuanwise.xiaoming.minecraft.xiaoming.listeners.ChannelListeners;
import cn.chuanwise.xiaoming.plugin.JavaPlugin;
import cn.chuanwise.xiaoming.user.XiaomingUser;
import io.netty.channel.ChannelFuture;
import lombok.Getter;

import java.io.File;
import java.util.Objects;

@Getter
@SuppressWarnings("all")
public class Plugin extends JavaPlugin {
    protected static final Plugin INSTANCE = new Plugin();
    public static Plugin getInstance() {
        return INSTANCE;
    }

    protected Configuration configuration;
    protected VerifyInteractors verifyInteractors;
    protected Server server;

    @Override
    public void onLoad() {
        final File dataFolder = getDataFolder();
        dataFolder.mkdirs();

        configuration = loadConfigurationOrSupply(Configuration.class, Configuration::new);
        verifyInteractors = new VerifyInteractors();
        server = new Server(this);
    }

    @Override
    public void onEnable() {
        // 启动服务器
        if (configuration.getConnection().isAutoBind()) {
            server.bind()
                    .orElseThrow()
                    .addListener(x -> {
                        if (x.isSuccess()) {
                            getLogger().info("成功在端口 " + configuration.getConnection().getPort() + " 上启动服务器");
                        } else {
                            getLogger().info("启动服务器失败");
                        }
                    });
        }

        // 注册监听器
        xiaomingBot.getEventManager().registerListeners(new ChannelListeners(), this);

        // 注册交互器参数解析器
        registerParameterParsers();

        // 注册交互器
        xiaomingBot.getInteractorManager().registerInteractors(new ConnectionInteractors(), this);
        xiaomingBot.getInteractorManager().registerInteractors(new StateInteractors(), this);
        xiaomingBot.getInteractorManager().registerInteractors(verifyInteractors, this);
    }

    @Override
    public void onDisable() {
        server.unbind().ifPresent(x -> x.addListener(y -> {
            if (y.isSuccess()) {
                getLogger().info("成功关闭在端口 " + configuration.getConnection().getPort() + " 上的服务器");
            } else {
                getLogger().info("关闭服务器失败");
            }
        }));
    }

    protected void registerParameterParsers() {
        xiaomingBot.getInteractorManager().registerParameterParser(OnlineClient.class, context -> {
            final XiaomingUser user = context.getUser();
            if (!server.isBound()) {
                user.sendError("服务器尚未启动！");
                return null;
            }

            final String inputValue = context.getInputValue();
            final Container<OnlineClient> optionalOnlineClient = CollectionUtil.findFirst(server.getOnlineClients(),
                    x -> Objects.equals(x.getServerInfo().getName(), inputValue));

            if (optionalOnlineClient.isEmpty()) {
                user.sendError("服务器" + inputValue + "并未连接到小明！");
                return null;
            }

            return Container.of(optionalOnlineClient.get());
        }, true, this);

        xiaomingBot.getInteractorManager().registerParameterParser(ServerInfo.class, context -> {
            final XiaomingUser user = context.getUser();
            final String inputValue = context.getInputValue();

            final ServerInfo serverInfo = configuration.getServers().get(inputValue);
            if (Objects.isNull(serverInfo)) {
                user.sendError("小明还不认识服务器「" + inputValue + "」");
                return null;
            }

            return Container.of(serverInfo);
        }, true, this);
    }
}