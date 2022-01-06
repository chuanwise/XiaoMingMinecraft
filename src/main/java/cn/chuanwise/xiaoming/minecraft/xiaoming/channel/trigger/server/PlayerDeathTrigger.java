package cn.chuanwise.xiaoming.minecraft.xiaoming.channel.trigger.server;

import cn.chuanwise.mclib.net.protocol.PlayerChatInform;
import cn.chuanwise.mclib.net.protocol.PlayerDeathInform;
import cn.chuanwise.xiaoming.minecraft.xiaoming.channel.trigger.TriggerHandleReceipt;
import cn.chuanwise.xiaoming.minecraft.xiaoming.event.PlayerDeathEvent;

import java.util.HashMap;
import java.util.Map;

public class PlayerDeathTrigger extends PlayerTrigger<PlayerDeathEvent> {
    @Override
    protected String getDescription1() {
        return "死亡触发器";
    }

    @Override
    protected TriggerHandleReceipt handle1(PlayerDeathEvent event) {
        final Map<String, Object> environment = new HashMap<>();
        environment.put("message", event.getMessage());
        return new TriggerHandleReceipt.Handled(environment, messages);
    }
}