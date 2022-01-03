package cn.chuanwise.xiaoming.minecraft.xiaoming.channel.trigger.xiaoming;

import cn.chuanwise.util.StringUtil;
import cn.chuanwise.xiaoming.contact.contact.GroupContact;
import cn.chuanwise.xiaoming.contact.contact.MemberContact;
import cn.chuanwise.xiaoming.minecraft.xiaoming.Plugin;
import cn.chuanwise.xiaoming.minecraft.xiaoming.channel.trigger.TriggerHandleReceipt;
import cn.chuanwise.xiaoming.minecraft.xiaoming.configuration.PlayerInfo;
import lombok.Data;
import net.mamoe.mirai.event.events.MemberMuteEvent;

import java.util.*;

@Data
public class MemberMuteTrigger extends QQTrigger<MemberMuteEvent> {
    String groupTag;

    @Override
    protected TriggerHandleReceipt handle0(MemberMuteEvent memberMuteEvent) {
        final Plugin plugin = Plugin.getInstance();
        final GroupContact groupContact = plugin.getXiaomingBot().getContactManager().getGroupContact(memberMuteEvent.getGroupId()).orElseThrow(NoSuchElementException::new);
        final MemberContact memberContact = groupContact.getMember(memberMuteEvent.getGroupId()).orElseThrow(NoSuchElementException::new);

        final boolean permissionAccepted = StringUtil.isEmpty(permission)
                || !plugin.getXiaomingBot().getPermissionService().hasPermission(memberContact.getCode(), permission);
        final boolean bindAccepted = !mustBind || plugin.getPlayerConfiguration().getPlayerInfo(memberContact.getCode()).isPresent();
        final boolean tagAccepted = memberContact.hasTag(accountTag) && groupContact.hasTag(groupTag);

        if (!permissionAccepted || !bindAccepted || !tagAccepted) {
            return TriggerHandleReceipt.Unhandled.getInstance();
        }

        return new TriggerHandleReceipt.Handled(Collections.emptyMap(), messages);
    }

    @Override
    public String getDescription() {
        return "群聊 #" + groupTag + " 成员 #" + accountTag + " 中的禁言触发器，" +
                "绑定要求：" + (mustBind ? "必须" : "不必") + "，" +
                "权限：" + (StringUtil.isEmpty(permission) ? "（无）" : permission) + "）";
    }
}