package cn.chuanwise.xiaoming.minecraft.xiaoming.channel.trigger.xiaoming;

import cn.chuanwise.common.util.Times;
import cn.chuanwise.xiaoming.contact.contact.GroupContact;
import cn.chuanwise.xiaoming.contact.contact.MemberContact;
import cn.chuanwise.xiaoming.contact.contact.XiaoMingContact;
import cn.chuanwise.xiaoming.contact.message.Message;
import cn.chuanwise.xiaoming.minecraft.xiaoming.XMMCXiaoMingPlugin;
import cn.chuanwise.xiaoming.minecraft.xiaoming.channel.trigger.TriggerHandleReceipt;
import lombok.Data;
import net.mamoe.mirai.event.events.MemberMuteEvent;

import java.util.*;
import java.util.concurrent.TimeUnit;

@Data
public class MemberMuteTrigger
        extends XiaoMingBindableTrigger<MemberMuteEvent> {
    String groupTag;

    @Override
    protected TriggerHandleReceipt handle1(MemberMuteEvent event) {
        final XMMCXiaoMingPlugin plugin = XMMCXiaoMingPlugin.getInstance();
        final GroupContact groupContact = plugin.getXiaoMingBot().getContactManager().getGroupContact(event.getGroupId()).orElseThrow(NoSuchElementException::new);
        final MemberContact memberContact = groupContact.getMember(event.getGroupId()).orElseThrow(NoSuchElementException::new);

        if (!memberContact.hasTag(accountTag) || !groupContact.hasTag(groupTag)) {
            return TriggerHandleReceipt.Unhandled.getInstance();
        }

        final Map<String, Object> environment = new HashMap<>();
        environment.put("groupTag", groupTag);
        environment.put("communicator()", groupContact);
        environment.put("member", memberContact);
        environment.put("duration", event.getDurationSeconds());
        environment.put("timeLength", Times.toTimeLength(TimeUnit.SECONDS.toMillis(event.getDurationSeconds())));
        return new TriggerHandleReceipt.Handled(environment);
    }

    @Override
    protected long getAccountCode(MemberMuteEvent memberMuteEvent) {
        return memberMuteEvent.getMember().getId();
    }
}