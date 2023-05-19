package cn.chuanwise.xiaoming.minecraft.xiaoming.channel.trigger.xiaoming;

import cn.chuanwise.common.util.Strings;
import cn.chuanwise.xiaoming.event.MessageEvent;
import cn.chuanwise.xiaoming.minecraft.xiaoming.channel.trigger.GroupTagTrigger;
import cn.chuanwise.xiaoming.minecraft.xiaoming.channel.trigger.TriggerHandleReceipt;
import cn.chuanwise.xiaoming.user.GroupXiaoMingUser;
import cn.chuanwise.xiaoming.user.XiaoMingUser;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.*;

@Data
@AllArgsConstructor
@NoArgsConstructor
@SuppressWarnings("all")
public class GroupMessageTrigger
        extends XiaoMingMessageTrigger
        implements GroupTagTrigger {
    String groupTag;

    @Override
    protected TriggerHandleReceipt handle2(MessageEvent event) {
        final XiaoMingUser xiaomingUser = event.getUser();
        if (!(xiaomingUser instanceof GroupXiaoMingUser)) {
            return TriggerHandleReceipt.Unhandled.getInstance();
        }
        if (!Strings.isEmpty(groupTag) && !xiaomingUser.getContact().hasTag(groupTag)) {
            return TriggerHandleReceipt.Unhandled.getInstance();
        }
        final GroupXiaoMingUser user = (GroupXiaoMingUser) xiaomingUser;

        final Map<String, Object> environment = new HashMap<>();
        environment.put("groupTag", groupTag);
        environment.put("groupCode", user.getGroupCode());
        return new TriggerHandleReceipt.Handled(environment);
    }
}