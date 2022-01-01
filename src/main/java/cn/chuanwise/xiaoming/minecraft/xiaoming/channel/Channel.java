package cn.chuanwise.xiaoming.minecraft.xiaoming.channel;

import cn.chuanwise.xiaoming.minecraft.xiaoming.channel.scope.Scope;
import cn.chuanwise.xiaoming.minecraft.xiaoming.channel.trigger.Trigger;
import lombok.Data;

import java.util.HashSet;
import java.util.Set;

@Data
public class Channel {
    String name;
    Set<Scope> scopes = new HashSet<>();
    Set<Trigger<?>> triggers = new HashSet<>();
}
