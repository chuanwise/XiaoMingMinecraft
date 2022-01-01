package cn.chuanwise.xiaoming.minecraft.xiaoming.channel.trigger;

import lombok.Data;

@Data
public abstract class ServerTrigger<T> extends Trigger<T> {
    protected String serverName;
}
