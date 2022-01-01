package cn.chuanwise.xiaoming.minecraft.xiaoming.channel.trigger;

import cn.chuanwise.xiaoming.event.MessageEvent;
import cn.chuanwise.xiaoming.user.PrivateXiaomingUser;
import cn.chuanwise.xiaoming.user.XiaomingUser;
import lombok.Data;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Data
public class PrivateMessageTrigger extends QQMessageTrigger {
    Set<Long> accountCodes = new HashSet<>();

    @Override
    protected List<String> handle0(MessageEvent event) {
        final XiaomingUser user = event.getUser();
        if (!(user instanceof PrivateXiaomingUser)) {
            return Collections.emptyList();
        }

        if (accountCodes.isEmpty()) {
            return messages;
        }
        final long accountCode = user.getCode();
        if (accountCodes.contains(accountCode)) {
            return messages;
        }
        return Collections.emptyList();
    }
}