package cn.chuanwise.xiaoming.minecraft.xiaoming.channel.trigger.server;

import cn.chuanwise.mclib.net.protocol.PlayerChatInform;
import cn.chuanwise.util.ConditionUtil;
import cn.chuanwise.xiaoming.minecraft.xiaoming.channel.trigger.MessageFilter;
import cn.chuanwise.xiaoming.minecraft.xiaoming.channel.trigger.MessageFilterReceipt;
import cn.chuanwise.xiaoming.minecraft.xiaoming.channel.trigger.MessageFilterTrigger;
import cn.chuanwise.xiaoming.minecraft.xiaoming.channel.trigger.TriggerHandleReceipt;
import lombok.Data;

import java.util.Map;

@Data
public class PlayerChatTrigger
        extends PlayerTrigger<PlayerChatInform>
        implements MessageFilterTrigger {
    MessageFilter messageFilter = new MessageFilter.All();

    @Override
    protected String getDescription1() {
        return "聊天触发器";
    }

    @Override
    protected TriggerHandleReceipt handle1(PlayerChatInform inform) {
        final String message = inform.getMessage();
        final MessageFilterReceipt filterReceipt = messageFilter.filter(message);

        if (filterReceipt instanceof MessageFilterReceipt.Denied) {
            return TriggerHandleReceipt.Unhandled.getInstance();
        }
        ConditionUtil.checkState(filterReceipt instanceof MessageFilterReceipt.Accepted, "internal error");
        final MessageFilterReceipt.Accepted accepted = (MessageFilterReceipt.Accepted) filterReceipt;

        final Map<String, Object> environment = accepted.getEnvironment();
        return new TriggerHandleReceipt.Handled(environment, messages);
    }
}
