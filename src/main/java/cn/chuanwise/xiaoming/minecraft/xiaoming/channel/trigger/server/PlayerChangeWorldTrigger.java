package cn.chuanwise.xiaoming.minecraft.xiaoming.channel.trigger.server;

import cn.chuanwise.mclib.net.protocol.PlayerChangeWorldInform;
import cn.chuanwise.xiaoming.minecraft.xiaoming.Plugin;

import java.util.HashMap;
import java.util.Map;

public class PlayerChangeWorldTrigger extends PlayerTrigger<PlayerChangeWorldInform> {
    @Override
    protected String getDescription1() {
        return "穿越世界触发器";
    }

    @Override
    protected Map<String, Object> buildEnvironment1(PlayerChangeWorldInform inform) {
        final Map<String, Object> environment = new HashMap<>();
        environment.put("from", inform.getFromWorldName());
        environment.put("to", inform.getToWorldName());
        return environment;
    }
}
