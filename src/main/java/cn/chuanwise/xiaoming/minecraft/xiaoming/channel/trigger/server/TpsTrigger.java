package cn.chuanwise.xiaoming.minecraft.xiaoming.channel.trigger.server;

import cn.chuanwise.xiaoming.minecraft.xiaoming.channel.trigger.TriggerHandleReceipt;
import cn.chuanwise.xiaoming.minecraft.xiaoming.event.ServerTpsEvent;
import lombok.Data;

import java.beans.Transient;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Data
public class TpsTrigger
        extends ServerTrigger<ServerTpsEvent> {
    int bottomTps = 18;
    long handleDelay = TimeUnit.MINUTES.toMillis(2);

    protected transient long lastHandledTimeMillis;
    @Transient
    public long getLastHandledTimeMillis() {
        return lastHandledTimeMillis;
    }

    @Override
    protected TriggerHandleReceipt handle1(ServerTpsEvent serverTpsEvent) {
        final int tps = serverTpsEvent.getTps();
        if (System.currentTimeMillis() - lastHandledTimeMillis < handleDelay) {
            return TriggerHandleReceipt.Unhandled.getInstance();
        }

        if (tps <= bottomTps) {
            final Map<String, Object> env = new HashMap<>();
            env.put("tps", tps);
            env.put("bottomTps", bottomTps);

            lastHandledTimeMillis = System.currentTimeMillis();
            return new TriggerHandleReceipt.Handled(env);
        } else {
            return TriggerHandleReceipt.Unhandled.getInstance();
        }
    }
}
