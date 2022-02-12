package cn.chuanwise.xiaoming.minecraft.bukkit.event;

import cn.chuanwise.mclib.PluginObject;
import cn.chuanwise.xiaoming.minecraft.bukkit.XMMCBukkitPlugin;

public interface XiaoMingMinecraftEvent extends PluginObject<XMMCBukkitPlugin> {
    default XMMCBukkitPlugin plugin() {
        return XMMCBukkitPlugin.getInstance();
    }
}
