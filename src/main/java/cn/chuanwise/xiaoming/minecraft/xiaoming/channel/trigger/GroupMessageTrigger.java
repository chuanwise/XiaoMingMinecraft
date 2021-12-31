package cn.chuanwise.xiaoming.minecraft.xiaoming.channel.trigger;

import lombok.Data;

import java.util.HashSet;
import java.util.Set;

@Data
public class GroupMessageTrigger extends QQMessageTrigger {
    Set<Long> groupCodes = new HashSet<>();
}
