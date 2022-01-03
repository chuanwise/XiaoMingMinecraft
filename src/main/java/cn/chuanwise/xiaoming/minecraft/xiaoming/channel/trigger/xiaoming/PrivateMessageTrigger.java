package cn.chuanwise.xiaoming.minecraft.xiaoming.channel.trigger.xiaoming;

import cn.chuanwise.util.StringUtil;
import cn.chuanwise.xiaoming.contact.contact.GroupContact;
import cn.chuanwise.xiaoming.contact.contact.PrivateContact;
import cn.chuanwise.xiaoming.event.MessageEvent;
import cn.chuanwise.xiaoming.group.GroupInformation;
import cn.chuanwise.xiaoming.minecraft.xiaoming.Plugin;
import cn.chuanwise.xiaoming.user.GroupXiaomingUser;
import cn.chuanwise.xiaoming.user.PrivateXiaomingUser;
import cn.chuanwise.xiaoming.user.XiaomingUser;
import lombok.Data;

import java.util.*;

@Data
@SuppressWarnings("all")
public class PrivateMessageTrigger extends QQMessageTrigger {
    @Override
    public String getDescription() {
        return "私聊 #" + accountTag + " 的消息触发器" +
                "（消息过滤器：" + messageFilter.getDescription() + "，" +
                "绑定要求：" + (mustBind ? "必须" : "不必") + "，" +
                "权限：" + (StringUtil.isEmpty(permission) ? "（无）" : permission) + "）";
    }
}