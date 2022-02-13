package cn.chuanwise.xiaoming.minecraft.bukkit.event;

import cn.chuanwise.mclib.plugin.PluginObject;
import cn.chuanwise.xiaoming.minecraft.bukkit.XMMCBukkitPlugin;

public interface XiaoMingMinecraftEvent
        extends PluginObject<XMMCBukkitPlugin> {

    default XMMCBukkitPlugin plugin() {
        return XMMCBukkitPlugin.getInstance();
    }
}
