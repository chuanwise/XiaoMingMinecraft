package cn.chuanwise.xiaoming.minecraft.xiaoming.channel.trigger;

import cn.chuanwise.util.ClassUtil;
import cn.chuanwise.util.ConditionUtil;
import cn.chuanwise.xiaoming.event.MessageEvent;
import lombok.Data;

import java.beans.Transient;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Data
public abstract class Trigger<T> {
    protected transient final Class<T> handledClass;

    @Transient
    public Class<T> getHandledClass() {
        return handledClass;
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    public Trigger() {
        this.handledClass = (Class) ClassUtil.getOnlyTypeParameterClass(getClass(), Trigger.class);
    }

    public Trigger(Class<T> handledClass) {
        ConditionUtil.notNull(handledClass, "handled class");
        this.handledClass = handledClass;
    }

    protected final List<String> messages = new ArrayList<>();

    @SuppressWarnings("unchecked")
    public final List<String> handle(Object t) {
        if (!handledClass.isInstance(t.getClass())) {
            return Collections.emptyList();
        }
        return handle0((T) t);
    }

    protected abstract List<String> handle0(T t);
}