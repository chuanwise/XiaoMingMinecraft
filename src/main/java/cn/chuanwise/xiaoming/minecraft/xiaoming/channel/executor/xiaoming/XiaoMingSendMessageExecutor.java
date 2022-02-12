package cn.chuanwise.xiaoming.minecraft.xiaoming.channel.executor.xiaoming;

import cn.chuanwise.xiaoming.message.MessageSendable;
import cn.chuanwise.xiaoming.minecraft.xiaoming.XMMCXiaoMingPlugin;
import cn.chuanwise.xiaoming.minecraft.xiaoming.channel.executor.FormatExecutor;
import lombok.Data;

import java.util.Map;

@Data
public class XiaoMingSendMessageExecutor
        extends XiaoMingExecutor
        implements FormatExecutor {
    String targetKey = "user";
    String format;

    @Override
    public void execute(Map<String, Object> environment) {
        final Object object = environment.get(targetKey);
        if (object instanceof MessageSendable) {
            final MessageSendable<?> messageSendable = (MessageSendable<?>) object;
            final XMMCXiaoMingPlugin plugin = XMMCXiaoMingPlugin.getInstance();
            final String finalMessage = plugin.getXiaoMingBot().getLanguageManager().formatAdditional(format, environment::get);

            messageSendable.sendMessage(finalMessage);
        }
    }
}
