package cn.chuanwise.xiaoming.minecraft.xiaoming.listeners;

import cn.chuanwise.xiaoming.annotation.EventListener;
import cn.chuanwise.xiaoming.contact.message.Message;
import cn.chuanwise.xiaoming.event.MessageEvent;
import cn.chuanwise.xiaoming.event.SimpleListeners;
import cn.chuanwise.xiaoming.minecraft.xiaoming.Plugin;
import cn.chuanwise.xiaoming.minecraft.xiaoming.channel.trigger.GroupMessageTrigger;
import cn.chuanwise.xiaoming.minecraft.xiaoming.channel.trigger.PrivateMessageTrigger;
import cn.chuanwise.xiaoming.minecraft.xiaoming.channel.trigger.QQTrigger;
import cn.chuanwise.xiaoming.minecraft.xiaoming.channel.trigger.Trigger;
import cn.chuanwise.xiaoming.minecraft.xiaoming.configuration.Configuration;
import cn.chuanwise.xiaoming.user.GroupXiaomingUser;
import cn.chuanwise.xiaoming.user.PrivateXiaomingUser;
import cn.chuanwise.xiaoming.user.XiaomingUser;

import java.util.Map;
import java.util.Objects;
import java.util.Set;

@SuppressWarnings("all")
public class ChannelInteractors extends SimpleListeners<Plugin> {
    @EventListener
    void onMessageEvent(MessageEvent event) {
        final Configuration configuration = plugin.getConfiguration();
        final Message message = event.getMessage();
        final XiaomingUser user = event.getUser();

        final Map<String, Trigger> triggers = configuration.getTriggers();

        configuration.getChannels().values().forEach(channel -> {
            boolean active = false;
            for (String triggerName : channel.getTriggerNames()) {
                final Trigger trigger = triggers.get(triggerName);
                if (Objects.isNull(trigger)) {
                    continue;
                }
                if (!(trigger instanceof QQTrigger)) {
                    continue;
                }

                if (trigger instanceof GroupMessageTrigger) {
                    if (!(user instanceof GroupXiaomingUser)) {
                        continue;
                    }
                    final long userGroupCode = ((GroupXiaomingUser) user).getGroupCode();
                    final Set<Long> triggerGroupCodes = ((GroupMessageTrigger) trigger).getGroupCodes();

                    if (triggerGroupCodes.contains(userGroupCode)) {
                        active = true;
                        break;
                    }
                    continue;
                }
                if (trigger instanceof PrivateMessageTrigger) {
                    if (!(user instanceof PrivateXiaomingUser)) {
                        continue;
                    }
                    final long accountCode = user.getCode();
                    final Set<Long> triggerAccountCode = ((PrivateMessageTrigger) trigger).getAccountCodes();

                    if (triggerAccountCode.contains(accountCode)) {
                        active = true;

                    }
                }
            }
        });
    }
}
