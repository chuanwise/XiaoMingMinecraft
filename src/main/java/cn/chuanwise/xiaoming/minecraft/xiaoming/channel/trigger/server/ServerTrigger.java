package cn.chuanwise.xiaoming.minecraft.xiaoming.channel.trigger.server;

import cn.chuanwise.util.Preconditions;
import cn.chuanwise.util.Strings;
import cn.chuanwise.xiaoming.minecraft.xiaoming.channel.trigger.ServerTagTrigger;
import cn.chuanwise.xiaoming.minecraft.xiaoming.channel.trigger.Trigger;
import cn.chuanwise.xiaoming.minecraft.xiaoming.channel.trigger.TriggerHandleReceipt;
import cn.chuanwise.xiaoming.minecraft.xiaoming.event.MinecraftEvent;
import cn.chuanwise.xiaoming.minecraft.xiaoming.net.OnlineClient;
import lombok.Data;

import java.util.HashMap;
import java.util.Map;

@Data
public abstract class ServerTrigger<T extends MinecraftEvent>
        extends Trigger<T>
        implements ServerTagTrigger {
    String serverTag;

    @Override
    protected final TriggerHandleReceipt handle0(T t) {
        final OnlineClient onlineClient = t.getOnlineClient();
        if (!Strings.isEmpty(serverTag) && !onlineClient.getServerInfo().hasTag(serverTag)) {
            return TriggerHandleReceipt.Unhandled.getInstance();
        }

        final TriggerHandleReceipt receipt = handle1(t);
        if (receipt instanceof TriggerHandleReceipt.Unhandled) {
            return receipt;
        }
        Preconditions.state(receipt instanceof TriggerHandleReceipt.Handled);
        final TriggerHandleReceipt.Handled handled = (TriggerHandleReceipt.Handled) receipt;

        final Map<String, Object> environment = new HashMap<>(handled.getEnvironment());
        environment.put("serverTag", serverTag);
        environment.put("server", onlineClient.getServerInfo());
        return new TriggerHandleReceipt.Handled(environment);
    }

    protected abstract TriggerHandleReceipt handle1(T t);
}
