package cn.chuanwise.xiaoming.minecraft.xiaoming.channel.trigger.xiaoming;

import cn.chuanwise.util.StringUtil;
import cn.chuanwise.xiaoming.contact.contact.GroupContact;
import cn.chuanwise.xiaoming.event.MessageEvent;
import cn.chuanwise.xiaoming.group.GroupInformation;
import cn.chuanwise.xiaoming.minecraft.xiaoming.Plugin;
import cn.chuanwise.xiaoming.minecraft.xiaoming.channel.trigger.GroupTagTrigger;
import cn.chuanwise.xiaoming.minecraft.xiaoming.channel.trigger.TriggerHandleReceipt;
import cn.chuanwise.xiaoming.user.GroupXiaomingUser;
import cn.chuanwise.xiaoming.user.XiaomingUser;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.*;

@Data
@AllArgsConstructor
@NoArgsConstructor
@SuppressWarnings("all")
public class GroupMessageTrigger
        extends QQMessageTrigger
        implements GroupTagTrigger {
    String groupTag;

    @Override
    public String getDescription() {
        return "群聊 #" + groupTag + " 成员 #" + accountTag + " 中的消息触发器" +
                "（消息过滤器：" + messageFilter.getDescription() + "，" +
                "绑定要求：" + (mustBind ? "必须" : "不必") + "，" +
                "权限：" + (StringUtil.isEmpty(permission) ? "（无）" : permission) + "）";
    }

    @Override
    protected TriggerHandleReceipt handle1(MessageEvent event) {
        final XiaomingUser xiaomingUser = event.getUser();
        if (!(xiaomingUser instanceof GroupXiaomingUser)
                || !xiaomingUser.getContact().hasTag(groupTag)) {
            return TriggerHandleReceipt.Unhandled.getInstance();
        }
        final GroupXiaomingUser user = (GroupXiaomingUser) xiaomingUser;

        final Map<String, Object> environment = new HashMap<>();
        environment.put("groupTag", groupTag);

        environment.put("accountCode", user.getCode());
        environment.put("groupCode", user.getGroupCode());

        final GroupContact contact = user.getContact();
        environment.put("contact", contact);
        environment.put("contactAlias", contact.getAlias());
        return new TriggerHandleReceipt.Handled(environment, messages);
    }
}