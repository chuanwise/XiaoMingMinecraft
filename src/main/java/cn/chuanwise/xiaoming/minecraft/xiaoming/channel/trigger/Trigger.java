package cn.chuanwise.xiaoming.minecraft.xiaoming.channel.trigger;

import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.util.internal.TypeParameterMatcher;
import lombok.AccessLevel;
import lombok.Data;
import lombok.Getter;

import java.beans.Transient;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Data
public abstract class Trigger {
    protected final List<String> messages = new ArrayList<>();

    @Getter(AccessLevel.NONE)
    protected transient final TypeParameterMatcher matcher;
    @Transient
    protected TypeParameterMatcher getMatcher() {
        return matcher;
    }

    public Trigger() {
        this.matcher = TypeParameterMatcher.find(this, Trigger.class, "T");
    }
}