package cn.chuanwise.xiaoming.minecraft.xiaoming.channel.trigger.server;

import cn.chuanwise.mclib.net.protocol.PlayerJoinInform;

public class PlayerJoinTrigger extends PlayerTrigger<PlayerJoinInform> {
    @Override
    protected String getDescription1() {
        return "上线触发器";
    }
}
