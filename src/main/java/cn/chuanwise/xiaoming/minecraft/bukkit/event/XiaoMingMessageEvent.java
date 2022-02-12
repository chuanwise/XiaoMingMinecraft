package cn.chuanwise.xiaoming.minecraft.bukkit.event;

import lombok.Data;

@Data
public class XiaoMingMessageEvent extends SimpleXiaoMingMinecraftEvent {
    protected final String message;
}
