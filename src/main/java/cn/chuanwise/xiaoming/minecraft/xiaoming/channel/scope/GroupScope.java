package cn.chuanwise.xiaoming.minecraft.xiaoming.channel.scope;

import cn.chuanwise.util.CollectionUtil;
import cn.chuanwise.xiaoming.minecraft.xiaoming.Plugin;
import lombok.Data;

import java.util.HashSet;
import java.util.Set;

@Data
public class GroupScope extends Scope {
    Set<Long> groupCodes = new HashSet<>();

    @Override
    public void sendMessage(Plugin plugin, String message) {
        groupCodes.forEach(x -> plugin.getXiaomingBot().getContactManager().sendGroupMessage(x, message));
    }

    @Override
    public String getDescription(Plugin plugin) {
        if (groupCodes.isEmpty()) {
            return "无任何群聊";
        } else {
            return "群聊" + CollectionUtil.toString(groupCodes, plugin.getXiaomingBot().getGroupInformationManager()::getAliasAndCode);
        }
    }
}