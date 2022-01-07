package cn.chuanwise.xiaoming.minecraft.xiaoming.channel.trigger;

import cn.chuanwise.util.TypeUtil;
import cn.chuanwise.util.ConditionUtil;
import lombok.Data;

import java.beans.Transient;
import java.util.ArrayList;
import java.util.List;

@Data
public abstract class Trigger<T> {
    protected transient final Class<T> handledClass;

    protected String name;

    @Transient
    public Class<T> getHandledClass() {
        return handledClass;
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    public Trigger() {
        this.handledClass = (Class) TypeUtil.getTypeParameterClass(getClass(), Trigger.class);
    }

    public Trigger(Class<T> handledClass) {
        ConditionUtil.notNull(handledClass, "handled class");
        this.handledClass = handledClass;
    }

    protected List<String> messages = new ArrayList<>();

    @SuppressWarnings("all")
    public final TriggerHandleReceipt handle(Object object) {
        if (!handledClass.isInstance(object)) {
            return TriggerHandleReceipt.Unhandled.getInstance();
        }
        return handle0((T) object);
    }

    protected abstract TriggerHandleReceipt handle0(T t);

    public abstract String getDescription();
}
