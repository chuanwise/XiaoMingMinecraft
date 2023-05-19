package cn.chuanwise.xiaoming.minecraft.xiaoming.channel.trigger.xiaoming;

import cn.chuanwise.common.util.Preconditions;
import cn.chuanwise.xiaoming.contact.contact.XiaoMingContact;
import cn.chuanwise.xiaoming.event.MessageEvent;
import cn.chuanwise.xiaoming.minecraft.xiaoming.XMMCXiaoMingPlugin;
import cn.chuanwise.xiaoming.minecraft.xiaoming.channel.trigger.MessageFilter;
import cn.chuanwise.xiaoming.minecraft.xiaoming.channel.trigger.MessageFilterReceipt;
import cn.chuanwise.xiaoming.minecraft.xiaoming.channel.trigger.MessageFilterTrigger;
import cn.chuanwise.xiaoming.minecraft.xiaoming.channel.trigger.TriggerHandleReceipt;
import cn.chuanwise.xiaoming.user.XiaoMingUser;
import lombok.Data;

import java.util.HashMap;
import java.util.Map;

@Data
@SuppressWarnings("all")
public abstract class XiaoMingMessageTrigger
    extends XiaoMingBindableTrigger<MessageEvent>
    implements MessageFilterTrigger {
    
    MessageFilter messageFilter = new MessageFilter.All();

    @Override
    protected final TriggerHandleReceipt handle1(MessageEvent event) {
        final XMMCXiaoMingPlugin plugin = XMMCXiaoMingPlugin.getInstance();
        final XiaoMingUser user = event.getUser();
        final String message = event.getMessage().serialize();

        // 过滤消息
        final MessageFilterReceipt filterReceipt = messageFilter.filter(message);
        if (filterReceipt instanceof MessageFilterReceipt.Denied) {
            return TriggerHandleReceipt.Unhandled.getInstance();
        }
        Preconditions.state(filterReceipt instanceof MessageFilterReceipt.Accepted);
        final MessageFilterReceipt.Accepted accepted = (MessageFilterReceipt.Accepted) filterReceipt;

        // 调用下级接收器
        final TriggerHandleReceipt receipt = handle2(event);
        if (receipt instanceof TriggerHandleReceipt.Unhandled) {
            return receipt;
        }
        Preconditions.state(receipt instanceof TriggerHandleReceipt.Handled);
        final TriggerHandleReceipt.Handled handled = (TriggerHandleReceipt.Handled) receipt;

        // 扩展环境
        final Map<String, Object> environment = new HashMap<>(handled.getEnvironment());
        environment.put("sender", user);
        environment.put("user", user);
        final XiaoMingContact communicator = user.getContact();
        environment.put("communicator()", communicator);
        environment.put("communicator()Alias", communicator.getAlias());
        environment.putAll(accepted.getEnvironment());
        return new TriggerHandleReceipt.Handled(environment);
    }

    protected abstract TriggerHandleReceipt handle2(MessageEvent event);

    @Override
    protected long getAccountCode(MessageEvent event) {
        return event.getUser().getCode();
    }
}