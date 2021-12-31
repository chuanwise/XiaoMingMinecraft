package cn.chuanwise.xiaoming.minecraft.xiaoming.channel.scope;

import cn.chuanwise.util.CollectionUtil;
import cn.chuanwise.xiaoming.minecraft.xiaoming.Plugin;
import lombok.Data;

import java.util.HashSet;
import java.util.Set;

@Data
public class PrivateScope extends Scope {
    Set<Long> accountCodes = new HashSet<>();

    @Override
    public void sendMessage(Plugin plugin, String message) {
        accountCodes.forEach(x -> plugin.getXiaomingBot().getContactManager().sendPrivateMessagePossibly(x, message));
    }

    @Override
    public String getDescription(Plugin plugin) {
        if (accountCodes.isEmpty()) {
            return "无任何用户";
        } else {
            return "与" + CollectionUtil.toString(accountCodes, plugin.getXiaomingBot().getAccountManager()::getAliasAndCode) + "的私聊";
        }
    }
}