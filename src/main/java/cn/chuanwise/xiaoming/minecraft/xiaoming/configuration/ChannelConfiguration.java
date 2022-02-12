package cn.chuanwise.xiaoming.minecraft.xiaoming.configuration;

import cn.chuanwise.xiaoming.minecraft.xiaoming.XMMCXiaoMingPlugin;
import cn.chuanwise.xiaoming.minecraft.xiaoming.channel.Channel;
import cn.chuanwise.xiaoming.preservable.SimplePreservable;
import lombok.Data;

import java.util.*;

@Data
@SuppressWarnings("all")
public class ChannelConfiguration extends SimplePreservable<XMMCXiaoMingPlugin> {
    Map<String, Channel> channels = new HashMap<>();

//    @Data
//    public static class DefaultValue {
//        String groupMessageTriggerMessage = "§7[§3{channel}§7] §b{sender} §8§l: §f{message}";
//        String playerChatTriggerMessage = "{player}：{message}";
//    }
//    DefaultValue defaultValue = new DefaultValue();

    public void channelHandle(Object object) {
        for (Channel channel : channels.values()) {
            channel.handle(object);
        }
    }
}
