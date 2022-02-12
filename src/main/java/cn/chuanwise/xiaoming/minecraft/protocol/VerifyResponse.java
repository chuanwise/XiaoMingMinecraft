package cn.chuanwise.xiaoming.minecraft.protocol;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

public class VerifyResponse {
    protected VerifyResponse() {}

    public static class Denied extends VerifyResponse {
        private static final Denied INSTANCE = new Denied();
        public static Denied getInstance() {
            return INSTANCE;
        }
        private Denied() {}
    }

    public static class Conflict extends VerifyResponse {
        private static final Conflict INSTANCE = new Conflict();
        public static Conflict getInstance() {
            return INSTANCE;
        }
        private Conflict() {}
    }

    public static class Cancelled extends VerifyResponse {
        private static final Cancelled INSTANCE = new Cancelled();
        public static Cancelled getInstance() {
            return INSTANCE;
        }
        private Cancelled() {}
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class Accepted extends VerifyResponse {
        String name;
    }

    @Data
    public static class Confirm extends VerifyResponse {
        @Data
        public static class Busy extends Confirm {}

        @Data
        @AllArgsConstructor
        @NoArgsConstructor
        public static class Operated extends Confirm {
            String verifyCode;
            long timeout;
        }
    }
}
