package cn.chuanwise.xiaoming.minecraft.xiaoming.channel;

import cn.chuanwise.xiaoming.minecraft.xiaoming.channel.scope.Scope;
import cn.chuanwise.xiaoming.minecraft.xiaoming.channel.trigger.Trigger;
import lombok.Data;

import java.util.*;

@Data
public class Channel {
    boolean enabled = true;
    String name;
    Set<Scope> scopes = new HashSet<>();
    Map<String, Trigger<?>> triggers = new HashMap<>();

    public Optional<Trigger<?>> getTrigger(String name) {
        return Optional.ofNullable(triggers.get(name));
    }
}