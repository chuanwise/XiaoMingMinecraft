package cn.chuanwise.xiaoming.minecraft.xiaoming.channel.trigger.xiaoming;


import cn.chuanwise.xiaoming.minecraft.xiaoming.channel.trigger.AccountTagTrigger;
import cn.chuanwise.xiaoming.minecraft.xiaoming.channel.trigger.Trigger;
import lombok.Data;

@Data
public abstract class QQTrigger<T>
        extends Trigger<T>
        implements AccountTagTrigger {
    String accountTag;
    boolean mustBind;
    String permission;
}
