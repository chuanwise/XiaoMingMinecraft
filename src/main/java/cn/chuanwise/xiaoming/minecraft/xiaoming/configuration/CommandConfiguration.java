package cn.chuanwise.xiaoming.minecraft.xiaoming.configuration;

import cn.chuanwise.xiaoming.minecraft.xiaoming.XMMCXiaoMingPlugin;
import cn.chuanwise.xiaoming.minecraft.xiaoming.command.Command;
import cn.chuanwise.xiaoming.preservable.SimplePreservable;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class CommandConfiguration extends SimplePreservable<XMMCXiaoMingPlugin> {
    final List<Command> commands = new ArrayList<>();
}
