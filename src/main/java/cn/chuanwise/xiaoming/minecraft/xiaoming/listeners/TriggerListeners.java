package cn.chuanwise.xiaoming.minecraft.xiaoming.listeners;

import cn.chuanwise.xiaoming.annotation.EventListener;
import cn.chuanwise.xiaoming.event.MessageEvent;
import cn.chuanwise.xiaoming.event.SimpleListeners;
import cn.chuanwise.xiaoming.minecraft.xiaoming.Plugin;
import net.mamoe.mirai.event.events.MemberMuteEvent;

public class TriggerListeners extends SimpleListeners<Plugin> {
    @EventListener
    void onMessageEvent(MessageEvent event) {
        plugin.getChannelConfiguration().channelHandle(event);
    }

    @EventListener
    void onMemberMute(MemberMuteEvent event) {
        plugin.getChannelConfiguration().channelHandle(event);
    }
}
