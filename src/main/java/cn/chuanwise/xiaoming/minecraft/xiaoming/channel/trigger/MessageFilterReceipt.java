package cn.chuanwise.xiaoming.minecraft.xiaoming.channel.trigger;

import lombok.Data;

import java.util.Map;

public class MessageFilterReceipt {
    private MessageFilterReceipt() {}

    @Data
    public static class Accepted extends MessageFilterReceipt {
        final Map<String, Object> environment;
        final String message;
    }

    public static class Denied extends MessageFilterReceipt {
        private static final Denied INSTANCE = new Denied();

        public static Denied getInstance() {
            return INSTANCE;
        }

        private Denied() {}
    }
}
