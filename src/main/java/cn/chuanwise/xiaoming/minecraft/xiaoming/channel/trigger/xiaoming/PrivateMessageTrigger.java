package cn.chuanwise.xiaoming.minecraft.xiaoming.channel.trigger.xiaoming;

import cn.chuanwise.xiaoming.event.MessageEvent;
import cn.chuanwise.xiaoming.minecraft.xiaoming.channel.trigger.TriggerHandleReceipt;
import cn.chuanwise.xiaoming.user.PrivateXiaoMingUser;
import lombok.Data;

import java.util.Collections;

@Data
@SuppressWarnings("all")
public class PrivateMessageTrigger extends XiaoMingMessageTrigger {
    @Override
    protected TriggerHandleReceipt handle2(MessageEvent event) {
        if (event.getUser() instanceof PrivateXiaoMingUser) {
            return new TriggerHandleReceipt.Handled(Collections.emptyMap());
        } else {
            return TriggerHandleReceipt.Unhandled.getInstance();
        }
    }
}