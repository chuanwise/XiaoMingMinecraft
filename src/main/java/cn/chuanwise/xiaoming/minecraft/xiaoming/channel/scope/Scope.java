package cn.chuanwise.xiaoming.minecraft.xiaoming.channel.scope;

import cn.chuanwise.xiaoming.minecraft.xiaoming.Plugin;

public abstract class Scope {
    public abstract void sendMessage(Plugin plugin, String message);

    public abstract String getDescription(Plugin plugin);
}
