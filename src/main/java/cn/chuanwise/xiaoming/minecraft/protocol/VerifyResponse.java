package cn.chuanwise.xiaoming.minecraft.protocol;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

public class VerifyResponse {
    protected VerifyResponse() {}

    @Data
    public static class Denied extends VerifyResponse {
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
