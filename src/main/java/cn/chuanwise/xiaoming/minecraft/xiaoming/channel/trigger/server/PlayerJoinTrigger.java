package cn.chuanwise.xiaoming.minecraft.xiaoming.channel.trigger.server;

import cn.chuanwise.xiaoming.minecraft.xiaoming.channel.trigger.TriggerHandleReceipt;
import cn.chuanwise.xiaoming.minecraft.xiaoming.event.PlayerJoinEvent;

import java.util.Collections;

public class PlayerJoinTrigger extends PlayerTrigger<PlayerJoinEvent> {
    @Override
    protected String getDescription1() {
        return "上线触发器";
    }

    @Override
    protected TriggerHandleReceipt handle1(PlayerJoinEvent event) {
        return new TriggerHandleReceipt.Handled(Collections.singletonMap("message", event.getMessage()), messages);
    }
}
