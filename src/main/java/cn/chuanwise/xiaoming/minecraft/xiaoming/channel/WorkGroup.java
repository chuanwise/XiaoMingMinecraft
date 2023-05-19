package cn.chuanwise.xiaoming.minecraft.xiaoming.channel;

import cn.chuanwise.common.util.Preconditions;
import cn.chuanwise.xiaoming.minecraft.xiaoming.channel.executor.Executor;
import cn.chuanwise.xiaoming.minecraft.xiaoming.channel.trigger.Trigger;
import cn.chuanwise.xiaoming.minecraft.xiaoming.channel.trigger.TriggerHandleReceipt;
import lombok.Data;

import java.util.*;

@Data
public class WorkGroup {
    /** 工作组名 */
    protected String name;

    protected Trigger<?> trigger;
    protected List<Executor> executors = new ArrayList<>();

    /**
     * 激发工作组，进行工作流程
     * @param object 激发对象
     * @return 工作组是否响应
     */
    public boolean work(Object object, Map<String, Object> environment) {
        final TriggerHandleReceipt receipt = trigger.handle(object);
        if (receipt instanceof TriggerHandleReceipt.Unhandled) {
            return false;
        }
        Preconditions.state(receipt instanceof TriggerHandleReceipt.Handled);
        final TriggerHandleReceipt.Handled handled = (TriggerHandleReceipt.Handled) receipt;

        final Map<String, Object> newEnvironment = new HashMap<>(handled.getEnvironment());
        newEnvironment.putAll(environment);
        newEnvironment.put("workGroup", this);

        for (Executor executor : executors) {
            executor.execute(newEnvironment);
        }

        return true;
    }

    public boolean work(Object object) {
        return work(object, Collections.emptyMap());
    }
}
