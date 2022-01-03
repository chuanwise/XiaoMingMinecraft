package cn.chuanwise.xiaoming.minecraft.xiaoming.channel.trigger.server;

import cn.chuanwise.mclib.net.protocol.PlayerChatInform;
import cn.chuanwise.mclib.net.protocol.PlayerDeathInform;

import java.util.HashMap;
import java.util.Map;

public class PlayerDeathTrigger extends PlayerTrigger<PlayerDeathInform> {
    @Override
    protected String getDescription1() {
        return "死亡触发器";
    }

    @Override
    protected Map<String, Object> buildEnvironment1(PlayerDeathInform inform) {
        final Map<String, Object> environment = new HashMap<>();
        environment.put("message", inform.getMessage());
        return environment;
    }
}