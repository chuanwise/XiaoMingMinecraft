package cn.chuanwise.xiaoming.minecraft.protocol;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
public class ConfirmRequest {
    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class Accepted extends ConfirmRequest {
        String name;
        String password;
    }

    public static class Denied extends ConfirmRequest {}
}