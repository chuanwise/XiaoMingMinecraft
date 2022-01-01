package cn.chuanwise.xiaoming.minecraft.xiaoming.channel.trigger;

import cn.chuanwise.xiaoming.event.MessageEvent;
import cn.chuanwise.xiaoming.user.GroupXiaomingUser;
import cn.chuanwise.xiaoming.user.XiaomingUser;
import lombok.Data;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Data
@SuppressWarnings("all")
public class GroupMessageTrigger extends QQMessageTrigger {
    Set<Long> groupCodes = new HashSet<>();

    @Override
    protected List<String> handle0(MessageEvent event) {
        final XiaomingUser user = event.getUser();
        if (!(user instanceof GroupXiaomingUser)) {
            return Collections.emptyList();
        }

        if (groupCodes.isEmpty()) {
            return messages;
        }

        final long groupCode = ((GroupXiaomingUser) user).getGroupCode();
        if (!groupCodes.contains(groupCode)) {
            return Collections.emptyList();
        }

        return messages;
    }
}