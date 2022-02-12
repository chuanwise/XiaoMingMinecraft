package cn.chuanwise.xiaoming.minecraft.xiaoming.configuration;

import cn.chuanwise.xiaoming.minecraft.xiaoming.XMMCXiaoMingPlugin;
import cn.chuanwise.xiaoming.preservable.SimplePreservable;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Data
public class PlayerVerifyCodeConfiguration extends SimplePreservable<XMMCXiaoMingPlugin> {
    long timeout = TimeUnit.DAYS.toMillis(5);

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class VerifyInfo {
        long timeMillis = System.currentTimeMillis();
        String playerName;
    }
    Map<String, VerifyInfo> verifyInfo = new HashMap<>();
}
