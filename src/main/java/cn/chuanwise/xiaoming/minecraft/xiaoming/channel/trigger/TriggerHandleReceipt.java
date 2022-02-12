package cn.chuanwise.xiaoming.minecraft.xiaoming.channel.trigger;

import lombok.Data;

import java.util.List;
import java.util.Map;

public class TriggerHandleReceipt {
    private TriggerHandleReceipt() {}

    @Data
    public static class Handled extends TriggerHandleReceipt {
        protected final Map<String, Object> environment;
    }

    public static class Unhandled extends TriggerHandleReceipt {
        private static final Unhandled INSTANCE = new Unhandled();

        public static Unhandled getInstance() {
            return INSTANCE;
        }

        private Unhandled() {}
    }
}
