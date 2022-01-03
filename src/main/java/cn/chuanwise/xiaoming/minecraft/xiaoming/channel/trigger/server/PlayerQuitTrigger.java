package cn.chuanwise.xiaoming.minecraft.xiaoming.channel.trigger.server;

import cn.chuanwise.mclib.net.protocol.PlayerQuitInform;

public class PlayerQuitTrigger extends PlayerTrigger<PlayerQuitInform> {
    @Override
    protected String getDescription1() {
        return "下线触发器";
    }
}
