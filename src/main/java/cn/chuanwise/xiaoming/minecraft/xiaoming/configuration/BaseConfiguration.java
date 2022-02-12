package cn.chuanwise.xiaoming.minecraft.xiaoming.configuration;

import cn.chuanwise.xiaoming.minecraft.xiaoming.XMMCXiaoMingPlugin;
import cn.chuanwise.xiaoming.preservable.SimplePreservable;
import lombok.Data;

@Data
public class BaseConfiguration extends SimplePreservable<XMMCXiaoMingPlugin> {
    boolean debug = false;

    @Data
    public static class Generators {
        StringGenerator verifyCode = new StringGenerator("0123456789", 4, 100);
        StringGenerator password = new StringGenerator("ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789!@#$%^&*()_+", 100, 100);
    }
    Generators generator = new Generators();
}
