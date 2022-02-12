package cn.chuanwise.xiaoming.minecraft.xiaoming.listeners;

import cn.chuanwise.xiaoming.annotation.EventListener;
import cn.chuanwise.xiaoming.event.MessageEvent;
import cn.chuanwise.xiaoming.event.SimpleListeners;
import cn.chuanwise.xiaoming.minecraft.xiaoming.XMMCXiaoMingPlugin;
import cn.chuanwise.xiaoming.minecraft.xiaoming.event.*;
import net.mamoe.mirai.event.events.MemberMuteEvent;

public class TriggerListeners extends SimpleListeners<XMMCXiaoMingPlugin> {
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
        plugin.getChannelConfiguration().channelHandle(event);
    }

    @EventListener
    void onPlayerDeath(PlayerDeathEvent event) {
        plugin.getChannelConfiguration().channelHandle(event);
    }

    @EventListener
    void onPlayerJoin(PlayerJoinEvent event) {
        plugin.getChannelConfiguration().channelHandle(event);
    }

    @EventListener
    void onPlayerQuit(PlayerQuitEvent event) {
        plugin.getChannelConfiguration().channelHandle(event);
    }

    @EventListener
    void onPlayerChangeWorld(PlayerChangeWorldEvent event) {
        plugin.getChannelConfiguration().channelHandle(event);
    }

    @EventListener
    void onPlayerChangeWorld(ServerTpsEvent event) {
        plugin.getChannelConfiguration().channelHandle(event);
    }

    @EventListener
    void onPlayerKick(PlayerKickEvent event) {
        plugin.getChannelConfiguration().channelHandle(event);
    }
}
