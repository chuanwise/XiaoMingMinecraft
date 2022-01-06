package cn.chuanwise.xiaoming.minecraft.xiaoming;

import cn.chuanwise.api.Logger;
import cn.chuanwise.toolkit.container.Container;
import cn.chuanwise.util.CollectionUtil;
import cn.chuanwise.xiaoming.language.LanguageManager;
import cn.chuanwise.xiaoming.minecraft.xiaoming.channel.Channel;
import cn.chuanwise.xiaoming.minecraft.xiaoming.channel.scope.Scope;
import cn.chuanwise.xiaoming.minecraft.xiaoming.channel.trigger.Trigger;
import cn.chuanwise.xiaoming.minecraft.xiaoming.configuration.*;
import cn.chuanwise.xiaoming.minecraft.xiaoming.interactors.*;
import cn.chuanwise.xiaoming.minecraft.xiaoming.listeners.TriggerListeners;
import cn.chuanwise.xiaoming.minecraft.xiaoming.net.OnlineClient;
import cn.chuanwise.xiaoming.minecraft.xiaoming.net.Server;
import cn.chuanwise.xiaoming.plugin.JavaPlugin;
import cn.chuanwise.xiaoming.user.XiaomingUser;
import lombok.Getter;

import java.io.File;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.concurrent.TimeoutException;

@Getter
@SuppressWarnings("all")
public class Plugin extends JavaPlugin {
    protected static final Plugin INSTANCE = new Plugin();

    public static Plugin getInstance() {
        return INSTANCE;
    }

    protected PluginConfiguration pluginConfiguration;
    protected PlayerConfiguration playerConfiguration;
    protected ChannelConfiguration channelConfiguration;

    protected VerifyInteractors verifyInteractors = new VerifyInteractors();
    private final CommandRequestInteractors interactors = new CommandRequestInteractors();

    protected Server server;
    protected Logger log;

    @Override
    public void onLoad() {
        final File dataFolder = getDataFolder();
        dataFolder.mkdirs();

        pluginConfiguration = loadConfigurationOrSupply(PluginConfiguration.class, PluginConfiguration::new);
        playerConfiguration = loadFileOrSupply(PlayerConfiguration.class, new File(dataFolder, "players.json"), PlayerConfiguration::new);
        channelConfiguration = loadFileOrSupply(ChannelConfiguration.class, new File(dataFolder, "channels.json"), ChannelConfiguration::new);

        log = new Logger() {
            @Override
            public void debug(String message) {
                if (pluginConfiguration.isDebug()) {
                    getLogger().info(message);
                }
            }

            @Override
            public void error(String message) {
                getLogger().error(message);
            }

            @Override
            public void info(String message) {
                getLogger().info(message);
            }

            @Override
            public void warn(String message) {
                getLogger().warn(message);
            }
        };

        server = new Server(this);
    }

    @Override
    public void onEnable() {
        // 启动服务器
        if (pluginConfiguration.getConnection().isAutoBind()) {
            server.bind()
                    .orElseThrow(NoSuchElementException::new)
                    .addListener(x -> {
                        if (x.isSuccess()) {
                            getLogger().info("成功在端口 " + pluginConfiguration.getConnection().getPort() + " 上启动服务器");
                        } else {
                            getLogger().info("启动服务器失败");
                        }
                    });
        }

        // 注册交互器参数解析器
        registerParameterParsers();

        // 注册交互器
        xiaomingBot.getInteractorManager().registerInteractors(new ConnectionInteractors(), this);
        xiaomingBot.getInteractorManager().registerInteractors(new StateInteractors(), this);
        xiaomingBot.getInteractorManager().registerInteractors(new ChannelInteractors(), this);
        xiaomingBot.getInteractorManager().registerInteractors(new CommandInteractors(), this);
        xiaomingBot.getInteractorManager().registerInteractors(new AccountInteractors(), this);
        xiaomingBot.getInteractorManager().registerInteractors(new ConfigurationInteractors(), this);
        xiaomingBot.getInteractorManager().registerInteractors(interactors, this);
        xiaomingBot.getInteractorManager().registerInteractors(verifyInteractors, this);

        xiaomingBot.getEventManager().registerListeners(new TriggerListeners(), this);
    }

    @Override
    public void onDisable() {
        server.unbind().ifPresent(x -> x.addListener(y -> {
            if (y.isSuccess()) {
                getLogger().info("成功关闭在端口 " + pluginConfiguration.getConnection().getPort() + " 上的服务器");
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
                user.sendError("「" + inputValue + "」并未连接到小明！");
                return null;
            }

            return Container.of(optionalOnlineClient.get());
        }, true, this);

        xiaomingBot.getInteractorManager().registerParameterParser(ServerInfo.class, context -> {
            final XiaomingUser user = context.getUser();
            final String inputValue = context.getInputValue();

            final ServerInfo serverInfo = pluginConfiguration.getServers().get(inputValue);
            if (Objects.isNull(serverInfo)) {
                user.sendError("小明还不认识服务器「" + inputValue + "」");
                return null;
            }

            return Container.of(serverInfo);
        }, true, this);

        xiaomingBot.getInteractorManager().registerThrowableCaughter(TimeoutException.class, (context, throwable) -> {
            final XiaomingUser user = context.getUser();
            user.sendError("请求超时");
        }, false,  this);

        final LanguageManager languageManager = xiaomingBot.getLanguageManager();
        languageManager.registerConvertor(Channel.class, Channel::getName, this);
        languageManager.registerOperators(Channel.class, this)
                .addOperator("name", Channel::getName)
                .addOperator("trigger", Channel::getTriggers)
                .addOperator("scope", Channel::getScopes);

        languageManager.registerConvertor(Trigger.class, Trigger::getName, this);
        languageManager.registerOperators(Trigger.class, this)
                .addOperator("name", Trigger::getName)
                .addOperator("description", Trigger::getDescription);

        languageManager.registerConvertor(Scope.class, Scope::getDescription, this);

        languageManager.registerOperators(ServerInfo.class, this)
                .addOperator("name", ServerInfo::getName);
        languageManager.registerConvertor(ServerInfo.class, ServerInfo::getName, this);

        languageManager.registerOperators(PlayerInfo.class, this)
                .addOperator("names", PlayerInfo::getPlayerNames)
                .addOperator("codes", PlayerInfo::getAccountCodes)
                .addOperator("name", x -> x.getPlayerNames().get(0));
        languageManager.registerConvertor(PlayerInfo.class, x -> x.getPlayerNames().get(0), this);
    }
}