package cn.chuanwise.xiaoming.minecraft.protocol;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

public class PlayerBindResponse {
    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class Wait extends PlayerBindResponse {
        String contactName;
        long timeout;
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class Error extends PlayerBindResponse {
        public enum Type {
            REPEAT,
            OTHER,
            FAILED,
        }
        Type type;
    }

}