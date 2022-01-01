package cn.chuanwise.xiaoming.minecraft.xiaoming.configuration;

import cn.chuanwise.xiaoming.minecraft.xiaoming.Plugin;
import cn.chuanwise.xiaoming.minecraft.xiaoming.channel.Channel;
import cn.chuanwise.xiaoming.preservable.SimplePreservable;
import lombok.Data;

import java.util.HashMap;
import java.util.Map;

@Data
public class ChannelConfiguration extends SimplePreservable<Plugin> {
    Map<String, Channel> channels = new HashMap<>();
}
