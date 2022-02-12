package cn.chuanwise.xiaoming.minecraft.xiaoming.channel.trigger.server;

import cn.chuanwise.xiaoming.minecraft.xiaoming.channel.trigger.TriggerHandleReceipt;
import cn.chuanwise.xiaoming.minecraft.xiaoming.event.PlayerChangeWorldEvent;

import java.util.HashMap;
import java.util.Map;

public class PlayerChangeWorldTrigger extends PlayerTrigger<PlayerChangeWorldEvent> {
    @Override
    protected TriggerHandleReceipt handle2(PlayerChangeWorldEvent event) {
        final Map<String, Object> environment = new HashMap<>();
        environment.put("fromWorld", event.getFromWorldName());
        environment.put("toWorld", event.getToWorldName());
        return new TriggerHandleReceipt.Handled(environment);
    }
}
