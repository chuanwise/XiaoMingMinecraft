package cn.chuanwise.xiaoming.minecraft.xiaoming.channel.scope;

import cn.chuanwise.util.CollectionUtil;
import cn.chuanwise.util.StringUtil;
import cn.chuanwise.xiaoming.contact.contact.GroupContact;
import cn.chuanwise.xiaoming.minecraft.xiaoming.Plugin;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class GroupScope extends QQScope {
    String groupTag;

    @Override
    public void sendMessage(List<String> messages) {
        Plugin.getInstance()
                .getXiaomingBot()
                .getGroupInformationManager()
                .searchGroupsByTag(groupTag)
                .forEach(x -> {
            x.getContact().ifPresent(y -> messages.forEach(y::sendMessage));
        });
    }

    @Override
    public String getDescription() {
        if (StringUtil.isEmpty(groupTag)) {
            return "无任何群聊";
        } else {
            return "带有标签 #" + groupTag + " 的群聊";
        }
    }
}