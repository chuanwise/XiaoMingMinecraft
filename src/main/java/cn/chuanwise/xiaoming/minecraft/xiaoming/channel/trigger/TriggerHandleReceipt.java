package cn.chuanwise.xiaoming.minecraft.xiaoming.channel.trigger;

import lombok.Data;

import java.util.List;
import java.util.Map;

public class TriggerHandleReceipt {
    private TriggerHandleReceipt() {}

    @Data
    public static class Handled extends TriggerHandleReceipt {
        final Map<String, Object> environment;
        final List<String> messages;
    }

    public static class Unhandled extends TriggerHandleReceipt {
        private static final Unhandled INSTANCE = new Unhandled();

        public static Unhandled getInstance() {
            return INSTANCE;
        }

        private Unhandled() {}
    }
}
