package cn.chuanwise.xiaoming.minecraft.xiaoming.channel.trigger.server;

import cn.chuanwise.xiaoming.minecraft.xiaoming.channel.trigger.TriggerHandleReceipt;
import cn.chuanwise.xiaoming.minecraft.xiaoming.event.PlayerQuitEvent;

import java.util.Collections;

public class PlayerQuitTrigger extends PlayerTrigger<PlayerQuitEvent> {
    @Override
    protected String getDescription1() {
        return "下线触发器";
    }

    @Override
    protected TriggerHandleReceipt handle1(PlayerQuitEvent event) {
        return new TriggerHandleReceipt.Handled(Collections.singletonMap("message", event.getMessage()), messages);
    }
}
