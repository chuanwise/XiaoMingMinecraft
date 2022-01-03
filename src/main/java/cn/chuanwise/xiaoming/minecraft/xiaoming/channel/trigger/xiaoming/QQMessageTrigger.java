package cn.chuanwise.xiaoming.minecraft.xiaoming.channel.trigger.xiaoming;

import cn.chuanwise.util.ConditionUtil;
import cn.chuanwise.util.StringUtil;
import cn.chuanwise.xiaoming.event.MessageEvent;
import cn.chuanwise.xiaoming.minecraft.xiaoming.Plugin;
import cn.chuanwise.xiaoming.minecraft.xiaoming.channel.trigger.MessageFilter;
import cn.chuanwise.xiaoming.minecraft.xiaoming.channel.trigger.MessageFilterReceipt;
import cn.chuanwise.xiaoming.minecraft.xiaoming.channel.trigger.MessageFilterTrigger;
import cn.chuanwise.xiaoming.minecraft.xiaoming.channel.trigger.TriggerHandleReceipt;
import cn.chuanwise.xiaoming.user.XiaomingUser;
import lombok.Data;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

@Data
@SuppressWarnings("all")
public abstract class QQMessageTrigger
        extends QQTrigger<MessageEvent>
        implements MessageFilterTrigger {
    MessageFilter messageFilter = new MessageFilter.All();

    @Override
    protected final TriggerHandleReceipt handle0(MessageEvent event) {
        final Plugin plugin = Plugin.getInstance();
        final XiaomingUser user = event.getUser();
        final String message = event.getMessage().serialize();

        // 先用 messageFilter
        final MessageFilterReceipt filterReceipt = messageFilter.filter(message);
        if (filterReceipt instanceof MessageFilterReceipt.Denied) {
            return TriggerHandleReceipt.Unhandled.getInstance();
        }
        ConditionUtil.checkState(filterReceipt instanceof MessageFilterReceipt.Accepted, "internal error");
        final MessageFilterReceipt.Accepted accepted = (MessageFilterReceipt.Accepted) filterReceipt;

        if (!user.hasTag(accountTag)
                || (mustBind && !plugin.getPlayerConfiguration().getPlayerInfo(user.getCode()).isPresent())
                || (!StringUtil.isEmpty(permission) && !user.hasPermission(permission))) {
            return TriggerHandleReceipt.Unhandled.getInstance();
        }

        final TriggerHandleReceipt receipt = handle1(event);
        if (receipt instanceof TriggerHandleReceipt.Unhandled) {
            return receipt;
        }
        ConditionUtil.checkState(receipt instanceof TriggerHandleReceipt.Handled, "internal error");
        final TriggerHandleReceipt.Handled handled = (TriggerHandleReceipt.Handled) receipt;

        final Map<String, Object> environment = new HashMap<>();

        environment.put("accountTag", accountTag);
        environment.put("mustBind", mustBind);
        environment.put("permission", permission);
        environment.put("sender", event.getUser());

        Plugin.getInstance()
                .getPlayerConfiguration()
                .getPlayerInfo(user.getCode())
                .ifPresent(x -> {
                    environment.put("player", x);
                });

        environment.putAll(handled.getEnvironment());
        environment.putAll(accepted.getEnvironment());

        return new TriggerHandleReceipt.Handled(environment, messages);
    }

    protected TriggerHandleReceipt handle1(MessageEvent event) {
        return new TriggerHandleReceipt.Handled(Collections.emptyMap(), messages);
    }
}