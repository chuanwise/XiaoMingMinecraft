package cn.chuanwise.xiaoming.minecraft.xiaoming.configuration;

import cn.chuanwise.xiaoming.minecraft.xiaoming.XMMCXiaoMingPlugin;
import cn.chuanwise.xiaoming.preservable.SimplePreservable;
import lombok.Data;

@Data
public class CommandRequestConfiguration extends SimplePreservable<XMMCXiaoMingPlugin> {
    boolean enable = false;
}
