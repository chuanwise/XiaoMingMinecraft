package cn.chuanwise.xiaoming.minecraft.xiaoming.configuration;

import cn.chuanwise.xiaoming.minecraft.xiaoming.channel.Channel;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
@AllArgsConstructor
public class ChannelHandlerReceipt {
    final Channel channel;
    final List<String> messages;
    final Map<String, Object> environment;
}
