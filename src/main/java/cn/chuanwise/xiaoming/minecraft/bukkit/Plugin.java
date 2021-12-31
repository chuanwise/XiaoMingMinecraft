package cn.chuanwise.xiaoming.minecraft.bukkit;

import cn.chuanwise.mclib.bukkit.BukkitPlugin;
import cn.chuanwise.mclib.bukkit.Commander;
import cn.chuanwise.mclib.storage.Language;
import cn.chuanwise.storage.file.StoredFile;
import cn.chuanwise.util.ConditionUtil;
import io.netty.channel.ChannelFuture;
import lombok.Getter;
import org.bstats.bukkit.Metrics;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.Optional;

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

    @Override
    public void onLoad0() {
        INSTANCE = this;
    }

    @Override
    protected void onEnable0() throws Exception {
        final File dataFolder = createDataFolder();

        final int pluginId = 12125;
        final Metrics metrics = new Metrics(this, pluginId);

        // 载入语言文件
        final File languageFile = new File(dataFolder, "language.yml");
        setupLanguage(languageFile, () -> StoredFile.loadYamlResource("language.yml", StandardCharsets.UTF_8, Language.class));

        configuration = setupConfiguration(Configuration.class, new File(getDataFolder(), "configuration.yml"), Configuration::new);
        client = new Client(this);

        // 注册监听器
        registerListeners(client.contact);

        // 尝试自动连接
        if (configuration.connection.autoConnect) {
            scheduler.runAsyncTask(() -> {
                final Optional<ChannelFuture> optional = client.connect();
                if (optional.isEmpty()) {
                    communicator.consoleError("net.connect.failed");
                    return;
                }
                optional.get().addListener(x -> {
                    if (x.isSuccess()) {
                        communicator.consoleInfo("net.connect.succeed");
                    } else {
                        communicator.consoleInfo("net.connect.failed");
                    }
                });
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
        communicator.setDebug(configuration.debug);
    }

    @Override
    protected void onDisable0() throws Exception {
        final Optional<ChannelFuture> optional = client.disconnectManually();
        if (optional.isPresent()) {
            optional.get().sync();
            communicator.consoleInfo("net.disconnect.succeed");
        }
    }
}