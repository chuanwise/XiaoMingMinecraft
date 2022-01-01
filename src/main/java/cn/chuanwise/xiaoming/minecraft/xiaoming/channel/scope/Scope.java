package cn.chuanwise.xiaoming.minecraft.xiaoming.channel.scope;

import cn.chuanwise.xiaoming.minecraft.xiaoming.Plugin;

import java.util.List;

public abstract class Scope {
    public abstract void sendMessage(Plugin plugin, List<String> messages);

    public abstract String getDescription(Plugin plugin);
}