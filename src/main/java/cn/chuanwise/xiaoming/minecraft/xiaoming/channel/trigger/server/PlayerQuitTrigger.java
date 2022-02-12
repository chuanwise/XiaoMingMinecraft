package cn.chuanwise.xiaoming.minecraft.xiaoming.channel.trigger.server;

import cn.chuanwise.xiaoming.minecraft.xiaoming.channel.trigger.TriggerHandleReceipt;
import cn.chuanwise.xiaoming.minecraft.xiaoming.event.PlayerQuitEvent;

import java.util.Collections;

public class PlayerQuitTrigger extends PlayerTrigger<PlayerQuitEvent> {
    @Override
    protected TriggerHandleReceipt handle2(PlayerQuitEvent event) {
        return new TriggerHandleReceipt.Handled(Collections.singletonMap("message", event.getMessage()));
    }
}
