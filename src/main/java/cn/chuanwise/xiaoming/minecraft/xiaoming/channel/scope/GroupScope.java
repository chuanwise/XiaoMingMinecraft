package cn.chuanwise.xiaoming.minecraft.xiaoming.channel.scope;

import cn.chuanwise.util.CollectionUtil;
import cn.chuanwise.xiaoming.minecraft.xiaoming.Plugin;
import lombok.Data;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Data
public class GroupScope extends Scope {
    Set<Long> groupCodes = new HashSet<>();

    @Override
    public void sendMessage(Plugin plugin, List<String> messages) {
        groupCodes.forEach(x -> messages.forEach(y -> plugin.getXiaomingBot().getContactManager().sendGroupMessage(x, y)));
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