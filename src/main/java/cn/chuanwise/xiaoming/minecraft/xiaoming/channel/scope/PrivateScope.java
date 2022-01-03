package cn.chuanwise.xiaoming.minecraft.xiaoming.channel.scope;

import cn.chuanwise.util.CollectionUtil;
import cn.chuanwise.util.StringUtil;
import cn.chuanwise.xiaoming.contact.contact.XiaomingContact;
import cn.chuanwise.xiaoming.minecraft.xiaoming.Plugin;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class PrivateScope extends QQScope {
    String accountTag;

    @Override
    public void sendMessage(List<String> messages) {
        final Plugin plugin = Plugin.getInstance();

        plugin.getXiaomingBot()
                .getAccountManager()
                .searchAccountsByTag(accountTag)
                .forEach(x -> {
            messages.forEach(y -> plugin.getXiaomingBot().getContactManager().sendPrivateMessagePossibly(x.getCode(), y));
        });
    }

    @Override
    public String getDescription() {
        if (StringUtil.isEmpty(accountTag)) {
            return "无任何用户";
        } else {
            return "带有标签 #" + accountTag + " 的用户";
        }
    }
}