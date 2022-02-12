package cn.chuanwise.xiaoming.minecraft.xiaoming.channel.executor.xiaoming;

import cn.chuanwise.xiaoming.minecraft.xiaoming.XMMCXiaoMingPlugin;
import cn.chuanwise.xiaoming.minecraft.xiaoming.channel.executor.GroupTagExecutor;
import cn.chuanwise.xiaoming.minecraft.xiaoming.channel.executor.FormatExecutor;
import lombok.Data;

import java.util.Map;

@Data
public class GroupBroadcastExecutor
        extends XiaoMingExecutor
        implements GroupTagExecutor, FormatExecutor {
    String groupTag;
    String format;

    @Override
    public void execute(Map<String, Object> environment) {
        environment.put("groupTag", groupTag);
        environment.put("format", format);

        // translate
        final XMMCXiaoMingPlugin plugin = XMMCXiaoMingPlugin.getInstance();
        final String finalMessage = plugin.getXiaoMingBot().getLanguageManager().formatAdditional(format, environment::get);

        // send
        plugin.getXiaoMingBot().getContactManager().sendGroupMessage(groupTag, finalMessage);
    }
}
