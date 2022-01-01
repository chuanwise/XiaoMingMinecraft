package cn.chuanwise.xiaoming.minecraft.xiaoming.channel.trigger;

import cn.chuanwise.util.CollectionUtil;
import cn.chuanwise.mclib.net.protocol.PlayerInform;
import cn.chuanwise.xiaoming.minecraft.xiaoming.Plugin;
import cn.chuanwise.xiaoming.minecraft.xiaoming.configuration.PlayerConfiguration;
import cn.chuanwise.xiaoming.minecraft.xiaoming.configuration.PlayerInfo;
import lombok.Data;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Data
public abstract class PlayerTrigger<T extends PlayerInform> extends ServerTrigger<T> {
    protected final Set<String> playerNames = new HashSet<>();

    protected final Set<Long> accountCodes = new HashSet<>();

    @Override
    protected final List<String> handle0(T t) {
        if (playerNames.isEmpty() && accountCodes.isEmpty()) {
            return messages;
        }

        final String playerName = t.getPlayerName();
        if (playerNames.contains(playerName)) {
            return messages;
        }

        final PlayerConfiguration playerConfiguration = Plugin.getInstance().getPlayerConfiguration();
        final Boolean containsAny = playerConfiguration.getPlayerInfo(t.getPlayerName())
                .map(PlayerInfo::getAccountCodes)
                .map(x -> CollectionUtil.containsAny(x, accountCodes))
                .orElse(false);

        if (containsAny) {
            return messages;
        } else {
            return handle1(t);
        }
    }

    protected List<String> handle1(T t) {
        return Collections.emptyList();
    }
}
