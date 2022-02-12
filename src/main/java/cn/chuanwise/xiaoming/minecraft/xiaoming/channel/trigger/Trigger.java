package cn.chuanwise.xiaoming.minecraft.xiaoming.channel.trigger;

import cn.chuanwise.util.Types;
import cn.chuanwise.util.Preconditions;
import cn.chuanwise.xiaoming.minecraft.xiaoming.channel.trigger.server.*;
import cn.chuanwise.xiaoming.minecraft.xiaoming.channel.trigger.xiaoming.GroupMessageTrigger;
import cn.chuanwise.xiaoming.minecraft.xiaoming.channel.trigger.xiaoming.MemberMuteTrigger;
import cn.chuanwise.xiaoming.minecraft.xiaoming.channel.trigger.xiaoming.PrivateMessageTrigger;
import lombok.Data;

import java.beans.Transient;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

@Data
public abstract class Trigger<T> {
    public static final Map<Class<?>, String> TRIGGER_NAMES;
    static {
        final Map<Class<?>, String> names = new HashMap<>();

        names.put(PlayerChangeWorldTrigger.class, "玩家穿越世界触发器");
        names.put(PlayerChatTrigger.class, "玩家聊天触发器");
        names.put(PlayerDeathTrigger.class, "玩家死亡触发器");
        names.put(PlayerJoinTrigger.class, "玩家上线触发器");
        names.put(PlayerQuitTrigger.class, "玩家下线触发器");
        names.put(TpsTrigger.class, "服务器 TPS 预警触发器");

        names.put(GroupMessageTrigger.class, "群聊消息触发器");
        names.put(PrivateMessageTrigger.class, "私聊消息触发器");
        names.put(MemberMuteTrigger.class, "群聊禁言触发器");

        TRIGGER_NAMES = Collections.unmodifiableMap(names);
    }

    /** 触发类型 */
    protected transient final Class<T> handledClass;
    @Transient
    public Class<T> getHandledClass() {
        return handledClass;
    }

    @SuppressWarnings("all")
    public Trigger() {
        this.handledClass = (Class) Types.getTypeParameterClass(getClass(), Trigger.class);
    }

    public Trigger(Class<T> handledClass) {
        Preconditions.nonNull(handledClass, "handled class");
        this.handledClass = handledClass;
    }

    @SuppressWarnings("all")
    public final TriggerHandleReceipt handle(Object object) {
        if (!handledClass.isInstance(object)) {
            return TriggerHandleReceipt.Unhandled.getInstance();
        }
        return handle0((T) object);
    }

    protected abstract TriggerHandleReceipt handle0(T t);
}
