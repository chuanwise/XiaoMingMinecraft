package cn.chuanwise.xiaoming.minecraft.xiaoming.channel.trigger;

import lombok.Data;

@Data
public abstract class ServerTrigger extends Trigger {
    protected String serverName;
}
