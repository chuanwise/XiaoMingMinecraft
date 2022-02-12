package cn.chuanwise.xiaoming.minecraft.xiaoming;

import cn.chuanwise.api.Logger;
import cn.chuanwise.toolkit.container.Container;
import cn.chuanwise.util.CollectionUtil;
import cn.chuanwise.xiaoming.language.LanguageManager;
import cn.chuanwise.xiaoming.minecraft.xiaoming.channel.Channel;
import cn.chuanwise.xiaoming.minecraft.xiaoming.channel.WorkGroup;
import cn.chuanwise.xiaoming.minecraft.xiaoming.channel.trigger.Trigger;
import cn.chuanwise.xiaoming.minecraft.xiaoming.configuration.*;
import cn.chuanwise.xiaoming.minecraft.xiaoming.interactors.*;
import cn.chuanwise.xiaoming.minecraft.xiaoming.listeners.NotificationListeners;
import cn.chuanwise.xiaoming.minecraft.xiaoming.listeners.TriggerListeners;
import cn.chuanwise.xiaoming.minecraft.xiaoming.net.OnlineClient;
import cn.chuanwise.xiaoming.minecraft.xiaoming.net.Server;
import cn.chuanwise.xiaoming.plugin.JavaPlugin;
import cn.chuanwise.xiaoming.user.XiaoMingUser;
import lombok.Getter;

import java.io.File;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.concurrent.TimeoutException;

@Getter
@SuppressWarnings("all")
public class XMMCXiaoMingPlugin extends JavaPlugin {
    protected static final XMMCXiaoMingPlugin INSTANCE = new XMMCXiaoMingPlugin();

    public static XMMCXiaoMingPlugin getInstance() {
        return INSTANCE;
    }

    protected SessionConfiguration sessionConfiguration;
    protected PlayerConfiguration playerConfiguration;
    protected ChannelConfiguration channelConfiguration;
    protected CommandConfiguration commandConfiguration;
    protected NotificationConfiguration notificationConfiguration;
    protected CommandRequestConfiguration commandRequestConfiguration;
    protected PlayerVerifyCodeConfiguration playerVerifyCodeConfiguration;
    protected BaseConfiguration baseConfiguration;

    protected VerifyInteractors verifyInteractors = new VerifyInteractors();
    private final CommandRequestInteractors interactors = new CommandRequestInteractors();

    protected Server server;
    protected Logger log;

    public void reload() {
        final File dataFolder = getDataFolder();
        dataFolder.mkdirs();

        sessionConfiguration = setupConfiguration(SessionConfiguration.class, SessionConfiguration::new);
        playerConfiguration = setupConfiguration(PlayerConfiguration.class, new File(dataFolder, "players.json"), PlayerConfiguration::new);
        channelConfiguration = setupConfiguration(ChannelConfiguration.class, new File(dataFolder, "channels.json"), ChannelConfiguration::new);
        commandConfiguration = setupConfiguration(CommandConfiguration.class, new File(dataFolder, "commands.json"), CommandConfiguration::new);
        notificationConfiguration = setupConfiguration(NotificationConfiguration.class, new File(dataFolder, "notifications.json"), NotificationConfiguration::new);
        commandRequestConfiguration = setupConfiguration(CommandRequestConfiguration.class, new File(dataFolder, "command-request.json"), CommandRequestConfiguration::new);
        playerVerifyCodeConfiguration = setupConfiguration(PlayerVerifyCodeConfiguration.class, new File(dataFolder, "player-verify-codes.json"), PlayerVerifyCodeConfiguration::new);
        baseConfiguration = setupConfiguration(BaseConfiguration.class, new File(dataFolder, "base.json"), BaseConfiguration::new);
    }

    @Override
    public void onLoad() {
        reload();

        log = new Logger() {
            @Override
            public void debug(String message) {
                if (baseConfiguration.isDebug()) {
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
        if (sessionConfiguration.getConnection().isAutoBind()) {
            server.bind()
                    .orElseThrow(NoSuchElementException::new)
                    .addListener(x -> {
                        if (x.isSuccess()) {
                            getLogger().info("成功在端口 " + sessionConfiguration.getConnection().getPort() + " 上启动服务器");
                        } else {
                            getLogger().info("启动服务器失败");
                        }
                    });
        }

        // 注册交互器参数解析器
        registerParameterParsers();

        // 注册交互器
        xiaoMingBot.getInteractorManager().registerInteractors(new ConnectionInteractors(), this);
        xiaoMingBot.getInteractorManager().registerInteractors(new StateInteractors(), this);
        xiaoMingBot.getInteractorManager().registerInteractors(new GuilderInteractors(), this);
        xiaoMingBot.getInteractorManager().registerInteractors(new ChannelInteractors(), this);
        xiaoMingBot.getInteractorManager().registerInteractors(new CommandInteractors(), this);
        xiaoMingBot.getInteractorManager().registerInteractors(new AccountInteractors(), this);
        xiaoMingBot.getInteractorManager().registerInteractors(new PlayerVerifyCodeInteractors(), this);
        xiaoMingBot.getInteractorManager().registerInteractors(new ConfigurationInteractors(), this);
        xiaoMingBot.getInteractorManager().registerInteractors(interactors, this);
        xiaoMingBot.getInteractorManager().registerInteractors(verifyInteractors, this);

        xiaoMingBot.getEventManager().registerListeners(new TriggerListeners(), this);
        xiaoMingBot.getEventManager().registerListeners(new NotificationListeners(), this);
    }

    @Override
    public void onDisable() {
        server.unbind().ifPresent(x -> x.addListener(y -> {
            if (y.isSuccess()) {
                getLogger().info("成功关闭在端口 " + sessionConfiguration.getConnection().getPort() + " 上的服务器");
            } else {
                getLogger().info("关闭服务器失败");
            }
        }));
    }

    protected void registerParameterParsers() {
        xiaoMingBot.getInteractorManager().registerParameterParser(OnlineClient.class, context -> {
            final XiaoMingUser user = context.getUser();
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

        xiaoMingBot.getInteractorManager().registerParameterParser(ServerInfo.class, context -> {
            final XiaoMingUser user = context.getUser();
            final String inputValue = context.getInputValue();

            final ServerInfo serverInfo = sessionConfiguration.getServers().get(inputValue);
            if (Objects.isNull(serverInfo)) {
                user.sendError("小明还不认识服务器「" + inputValue + "」");
                return null;
            }

            return Container.of(serverInfo);
        }, true, this);

        xiaoMingBot.getInteractorManager().registerThrowableCaughter(TimeoutException.class, (context, throwable) -> {
            final XiaoMingUser user = context.getUser();
            user.sendError("请求超时");
        }, false,  this);

        final LanguageManager languageManager = xiaoMingBot.getLanguageManager();
        languageManager.registerConvertor(Channel.class, Channel::getName, this);
        languageManager.registerOperators(Channel.class, this)
                .addOperator("enabled", Channel::isEnable)
                .addOperator("name", Channel::getName)
                .addOperator("workGroups", Channel::getWorkGroups);

        languageManager.registerConvertor(OnlineClient.class, x -> x.getServerInfo().getName(), this);
        languageManager.registerOperators(OnlineClient.class, this)
                        .addOperator("name", x -> x.getServerInfo().getName())
                        .addOperator("info", OnlineClient::getServerInfo)
                        .addOperator("onlinePlayerCount", x -> {
                            try {
                                return x.getRemoteContact().getOnlinePlayers().size();
                            } catch (Exception exception) {
                                return null;
                            }
                        });

        languageManager.registerConvertor(WorkGroup.class, WorkGroup::getName, this);
        languageManager.registerOperators(WorkGroup.class, this)
                .addOperator("trigger", WorkGroup::getTrigger)
                .addOperator("executors", WorkGroup::getExecutors);

        languageManager.registerConvertor(Trigger.class, x -> Trigger.TRIGGER_NAMES.get(x.getClass()), this);
        languageManager.registerOperators(Trigger.class, this)
                .addOperator("name", x -> Trigger.TRIGGER_NAMES.get(x.getClass()));

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