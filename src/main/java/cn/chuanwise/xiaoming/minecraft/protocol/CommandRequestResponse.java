package cn.chuanwise.xiaoming.minecraft.protocol;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

public class CommandRequestResponse {
    private CommandRequestResponse() {}

    public static class Undefined extends CommandRequestResponse {
        private Undefined() {}
        private static final Undefined INSTANCE = new Undefined();
        public static Undefined getInstance() {
            return INSTANCE;
        }
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class Operated extends CommandRequestResponse {
        long operatorCode;
        boolean accepted;
    }

    public static class Timeout extends CommandRequestResponse {
        private Timeout() {}
        private static final Timeout INSTANCE = new Timeout();
        public static Timeout getInstance() {
            return INSTANCE;
        }
    }
}
