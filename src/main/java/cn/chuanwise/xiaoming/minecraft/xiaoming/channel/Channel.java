package cn.chuanwise.xiaoming.minecraft.xiaoming.channel;

import lombok.Data;

import java.util.HashSet;
import java.util.Set;

@Data
public class Channel {
    String name;
    Set<String> triggerNames = new HashSet<>();
}
