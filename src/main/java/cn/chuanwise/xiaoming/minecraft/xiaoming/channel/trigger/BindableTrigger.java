package cn.chuanwise.xiaoming.minecraft.xiaoming.channel.trigger;

import cn.chuanwise.xiaoming.minecraft.xiaoming.channel.trigger.AccountTagTrigger;
import cn.chuanwise.xiaoming.minecraft.xiaoming.channel.trigger.PlayerPermissionTrigger;
import cn.chuanwise.xiaoming.minecraft.xiaoming.channel.trigger.XiaoMingPermissionTrigger;
import org.bukkit.Bukkit;

public interface BindableTrigger extends AccountTagTrigger, XiaoMingPermissionTrigger, PlayerPermissionTrigger {
    boolean isMustBind();

    void setMustBind(boolean mustBind);
}