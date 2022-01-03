package cn.chuanwise.xiaoming.minecraft.xiaoming.channel.trigger.server;

import cn.chuanwise.xiaoming.minecraft.xiaoming.channel.trigger.ServerTagTrigger;
import cn.chuanwise.xiaoming.minecraft.xiaoming.channel.trigger.Trigger;
import lombok.Data;

@Data
public abstract class ServerTrigger<T>
        extends Trigger<T>
        implements ServerTagTrigger {
    String serverTag;
}
