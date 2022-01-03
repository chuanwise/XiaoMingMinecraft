package cn.chuanwise.xiaoming.minecraft.bukkit;

import cn.chuanwise.mclib.bukkit.BukkitPlugin;
import cn.chuanwise.mclib.bukkit.Commander;
import cn.chuanwise.mclib.bukkit.ask.Asker;
import cn.chuanwise.mclib.bukkit.ask.AskerManager;
import cn.chuanwise.mclib.bukkit.util.CommandUtil;
import cn.chuanwise.mclib.storage.Language;
import cn.chuanwise.storage.file.StoredFile;
import cn.chuanwise.util.ConditionUtil;
import cn.chuanwise.util.StreamUtil;
import cn.chuanwise.util.ThrowableUtil;
import cn.chuanwise.xiaoming.minecraft.bukkit.net.Client;
import io.netty.channel.ChannelFuture;
import lombok.Getter;
import org.bstats.bukkit.Metrics;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;

@Getter
public class Plugin extends BukkitPlugin {
    protected static Plugin INSTANCE;
    public static Plugin getInstance() {
        ConditionUtil.checkState(Objects.nonNull(INSTANCE), "插件尚未加载！");
        return INSTANCE;
    }

    protected Configuration configuration;
    protected Client client;

    protected PluginCommands commands;
    protected AskerManager askerManager = new AskerManager();

    @Override
    public void onLoad0() {
        INSTANCE = this;
    }

    public void reload() throws IOException {
        final File dataFolder = createDataFolder();

        // 载入语言文件
        final File languageFile = new File(dataFolder, "language.yml");
        setupLanguage(languageFile, () -> StoredFile.loadYamlResource("language.yml", StandardCharsets.UTF_8, Language.class));

        configuration = setupConfiguration(Configuration.class, new File(getDataFolder(), "configuration.yml"), Configuration::new);
        communicator.setDebug(configuration.isDebug());
    }

    @Override
    protected void onEnable0() throws Exception {
        // 读取 enable 消息
        try {
            final String enableMessage = StreamUtil.read(getClassLoader().getResourceAsStream("message/enable.txt"), StandardCharsets.UTF_8);
            getServer().getConsoleSender().sendMessage(enableMessage);
        } catch (IOException exception) {
            communicator.consoleInfoString("欢迎使用小明 b（￣▽￣）d");
        }

        final int pluginId = 12125;
        final Metrics metrics = new Metrics(this, pluginId);

        reload();

        client = new Client(this);
        registerListeners(client.getLocalContact());

        // 尝试自动连接
        if (configuration.connection.autoConnect) {
            scheduler.runAsyncTask(() -> {
                client.connect().ifPresent(x -> x.addListener(y -> {
                    if (y.isSuccess()) {
                        communicator.consoleInfo("net.connect.succeed");
                    } else {
                        communicator.consoleWarn("net.connect.failed");
                    }
                }));
            });
        }

        // 注册指令
        commands = new PluginCommands(this);
        new Commander(this).commandBuilder()
                .name("xiaomingminecraft")
                .aliases("xmmc", "xiaomingmc", "xm")
                .commands(commands)
                .build()
                .register();

        setupAskModule();
    }

    private void setupAskModule() {
        final Command acceptCommand = new Command("xiaomingaccept",
                "同意请求", "/xmaccept", Collections.singletonList("xmaccept")) {
            @Override
            public boolean execute(CommandSender commandSender, String s, String[] strings) {
                final Optional<Asker> optionalAsker = askerManager.getAsker(commandSender);
                if (!optionalAsker.isPresent()) {
                    communicator.warn(commandSender, "ask.free");
                } else {
                    optionalAsker.get().accept();
                    communicator.info(commandSender, "ask.accepted");
                }
                return false;
            }
        };
        final Command denyCommand = new Command("xiaomingdeny",
                "拒绝请求", "/xmdeny", Collections.singletonList("xmdeny")) {
            @Override
            public boolean execute(CommandSender commandSender, String s, String[] strings) {
                final Optional<Asker> optionalAsker = askerManager.getAsker(commandSender);
                if (!optionalAsker.isPresent()) {
                    communicator.warn(commandSender, "ask.free");
                } else {
                    optionalAsker.get().deny();
                    communicator.info(commandSender, "ask.denied");
                }
                return false;
            }
        };
        final Command ignoreCommand = new Command("xiaomingignore",
                "拒绝请求", "/xmignore", Collections.singletonList("xmignore")) {
            @Override
            public boolean execute(CommandSender commandSender, String s, String[] strings) {
                final Optional<Asker> optionalAsker = askerManager.getAsker(commandSender);
                if (!optionalAsker.isPresent()) {
                    communicator.warn(commandSender, "ask.free");
                } else {
                    optionalAsker.get().ignore();
                    communicator.info(commandSender, "ask.ignored");
                }
                return false;
            }
        };
        CommandUtil.registerCommand(acceptCommand, this);
        CommandUtil.registerCommand(denyCommand, this);
        CommandUtil.registerCommand(ignoreCommand, this);
    }

    @Override
    protected void onDisable0() throws Exception {
        final Optional<ChannelFuture> optional = client.disconnectManually();
        if (optional.isPresent()) {
            optional.get().sync();
            communicator.consoleInfo("net.disconnect.succeed");
        }

        // 读取 enable 消息
        try {
            final String enableMessage = StreamUtil.read(getClassLoader().getResourceAsStream("message/disable.txt"), StandardCharsets.UTF_8);
            getServer().getConsoleSender().sendMessage(enableMessage);
        } catch (IOException exception) {
            communicator.consoleInfoString("期待我们的下一次重逢！");
        }
    }
}