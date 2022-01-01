package cn.chuanwise.xiaoming.minecraft.xiaoming.configuration;

import cn.chuanwise.util.CollectionUtil;
import cn.chuanwise.util.ConditionUtil;
import cn.chuanwise.xiaoming.minecraft.xiaoming.Plugin;
import cn.chuanwise.xiaoming.preservable.SimplePreservable;
import lombok.Data;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

@Data
public class PlayerConfiguration extends SimplePreservable<Plugin> {
    Set<PlayerInfo> players = new HashSet<>();

    public Optional<PlayerInfo> getPlayerInfo(long accountCode) {
        return CollectionUtil.findFirst(players, x -> x.hasAccountCode(accountCode)).toOptional();
    }

    public Optional<PlayerInfo> getPlayerInfo(String playerName) {
        ConditionUtil.notNull(playerName, "player name");
        return CollectionUtil.findFirst(players, x -> x.hasPlayerName(playerName)).toOptional();
    }
}