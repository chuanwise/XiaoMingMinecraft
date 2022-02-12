package cn.chuanwise.xiaoming.minecraft.xiaoming.interactors;

import cn.chuanwise.xiaoming.annotation.Filter;
import cn.chuanwise.xiaoming.annotation.FilterParameter;
import cn.chuanwise.xiaoming.annotation.Required;
import cn.chuanwise.xiaoming.interactor.SimpleInteractors;
import cn.chuanwise.xiaoming.minecraft.xiaoming.XMMCXiaoMingPlugin;
import cn.chuanwise.xiaoming.minecraft.xiaoming.configuration.BaseConfiguration;
import cn.chuanwise.xiaoming.minecraft.xiaoming.configuration.ChannelConfiguration;
import cn.chuanwise.xiaoming.minecraft.xiaoming.configuration.PlayerConfiguration;
import cn.chuanwise.xiaoming.minecraft.xiaoming.configuration.SessionConfiguration;
import cn.chuanwise.xiaoming.minecraft.xiaoming.net.Server;
import cn.chuanwise.xiaoming.minecraft.xiaoming.util.Words;
import cn.chuanwise.xiaoming.user.XiaoMingUser;

import java.io.IOException;

@SuppressWarnings("all")
public class ConfigurationInteractors extends SimpleInteractors<XMMCXiaoMingPlugin> {
    @Filter(Words.SET + Words.SERVER + Words.PORT + " {端口}")
    @Required("xmmc.admin.port")
    void setPort(XiaoMingUser user, @FilterParameter("端口") int port) {
        final SessionConfiguration sessionConfiguration = plugin.getSessionConfiguration();
        final SessionConfiguration.Connection connection = sessionConfiguration.getConnection();

        connection.setPort(port);
        final Server server = plugin.getServer();

        if (server.isBound()) {
            user.sendMessage("成功修改服务器端口为「" + port + "」，重启后生效");
        } else {
            user.sendMessage("成功修改服务器端口为「" + port + "」");
        }
        sessionConfiguration.readyToSave();
    }

    @Filter(Words.DEBUG + Words.SERVER)
    @Required("xmmc.admin.debug")
    void debug(XiaoMingUser user) {
        final BaseConfiguration configuration = plugin.getBaseConfiguration();
        final boolean setTo = !configuration.isDebug();
        configuration.setDebug(setTo);
        configuration.readyToSave();

        if (setTo) {
            user.sendMessage("成功启动调试模式");
        } else {
            user.sendMessage("成功关闭调试模式");
        }
    }

    @Filter(Words.RELOAD + Words.XIAOMING + Words.MINECRAFT)
    @Required("xmmc.admin.reload")
    void reload(XiaoMingUser user) {
        plugin.reload();
        user.sendMessage("成功重新载入 XiaoMingMinecraft 插件数据");
    }

    @Filter(Words.SAVE + Words.CONFIGURE)
    @Required("xmmc.admin.config.save")
    void saveConfig(XiaoMingUser user) throws IOException {
        final ChannelConfiguration channelConfiguration = plugin.getChannelConfiguration();
        final PlayerConfiguration playerConfiguration = plugin.getPlayerConfiguration();
        final SessionConfiguration sessionConfiguration = plugin.getSessionConfiguration();

        channelConfiguration.save();
        playerConfiguration.save();
        sessionConfiguration.save();
        user.sendMessage("成功保存 XiaoMingMinecraft 的所有配置文件");
    }
}