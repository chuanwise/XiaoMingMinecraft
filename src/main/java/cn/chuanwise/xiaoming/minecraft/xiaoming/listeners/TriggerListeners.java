package cn.chuanwise.xiaoming.minecraft.xiaoming.listeners;

import cn.chuanwise.xiaoming.annotation.EventListener;
import cn.chuanwise.xiaoming.event.MessageEvent;
import cn.chuanwise.xiaoming.event.SimpleListeners;
import cn.chuanwise.xiaoming.minecraft.xiaoming.Plugin;
import cn.chuanwise.xiaoming.minecraft.xiaoming.event.*;
import net.mamoe.mirai.event.events.MemberMuteEvent;

public class TriggerListeners extends SimpleListeners<Plugin> {
    /** qq triggers */
    @EventListener
    void onMessageEvent(MessageEvent event) {
        plugin.getChannelConfiguration().channelHandle(event);
    }

    @EventListener
    void onMemberMute(MemberMuteEvent event) {
        plugin.getChannelConfiguration().channelHandle(event);
    }

    /** server triggers */
    @EventListener
    void onPlayerChat(PlayerChatEvent event) {
        plugin.getChannelConfiguration().channelHandle(event, event.getOnlineClient());
    }

    @EventListener
    void onPlayerDeath(PlayerDeathEvent event) {
        plugin.getChannelConfiguration().channelHandle(event, event.getOnlineClient());
    }

    @EventListener
    void onPlayerJoin(PlayerJoinEvent event) {
        plugin.getChannelConfiguration().channelHandle(event, event.getOnlineClient());
    }

    @EventListener
    void onPlayerQuit(PlayerQuitEvent event) {
        plugin.getChannelConfiguration().channelHandle(event, event.getOnlineClient());
    }

    @EventListener
    void onPlayerKick(PlayerKickEvent event) {
        plugin.getChannelConfiguration().channelHandle(event, event.getOnlineClient());
    }
}
