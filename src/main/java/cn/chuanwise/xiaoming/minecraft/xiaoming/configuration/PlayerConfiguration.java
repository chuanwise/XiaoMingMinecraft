package cn.chuanwise.xiaoming.minecraft.xiaoming.configuration;

import cn.chuanwise.util.CollectionUtil;
import cn.chuanwise.util.ConditionUtil;
import cn.chuanwise.xiaoming.minecraft.xiaoming.Plugin;
import cn.chuanwise.xiaoming.preservable.SimplePreservable;
import lombok.Data;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;

@Data
public class PlayerConfiguration extends SimplePreservable<Plugin> {
    long boundTimeout = TimeUnit.MINUTES.toMillis(1);

    Set<PlayerInfo> players = new HashSet<>();
    boolean allowBind = true;

    public Optional<PlayerInfo> getPlayerInfo(long accountCode) {
        return CollectionUtil.findFirst(players, x -> x.hasAccountCode(accountCode)).toOptional();
    }

    public Optional<PlayerInfo> getPlayerInfo(String playerName) {
        ConditionUtil.notNull(playerName, "player name");
        return CollectionUtil.findFirst(players, x -> x.hasPlayerName(playerName)).toOptional();
    }

    // TODO: 2022/1/4 配置绑定失败消息
    public enum BindReceipt {
        SUCCEED,
        REPEAT,
        OTHER,
        DENIED,
    }
    public BindReceipt forceBind(long accountCode, String playerName) {
        final Optional<PlayerInfo> optionalSameCodePlayerInfo = getPlayerInfo(accountCode);
        if (optionalSameCodePlayerInfo.isPresent()) {
            final PlayerInfo playerInfo = optionalSameCodePlayerInfo.get();;
            if (playerInfo.hasPlayerName(playerName)) {
                return BindReceipt.REPEAT;
            }
        }

        final Optional<PlayerInfo> optionalSameNamePlayerInfo = getPlayerInfo(playerName);
        if (optionalSameNamePlayerInfo.isPresent()) {
            final PlayerInfo playerInfo = optionalSameNamePlayerInfo.get();
            if (playerInfo.hasAccountCode(accountCode)) {
                throw new IllegalStateException();
            }
            return BindReceipt.OTHER;
        }

        final PlayerInfo playerInfo;
        if (optionalSameCodePlayerInfo.isPresent()) {
            playerInfo = optionalSameCodePlayerInfo.get();
        } else {
            playerInfo = new PlayerInfo();
            players.add(playerInfo);
            playerInfo.accountCodes.add(accountCode);
        }
        playerInfo.playerNames.add(playerName);

        return BindReceipt.SUCCEED;
    }

    public boolean unbind(long accountCode, String playerName) {
        final Optional<PlayerInfo> optionalPlayerInfo = getPlayerInfo(accountCode);
        if (optionalPlayerInfo.isPresent()) {
            final PlayerInfo playerInfo = optionalPlayerInfo.get();
            if (playerInfo.playerNames.remove(playerName)) {
                if (playerInfo.playerNames.isEmpty()) {
                    players.remove(playerInfo);
                }
                return true;
            } else {
                return false;
            }
        }

        return false;
    }

    public boolean unbind(String playerName) {
        ConditionUtil.notNull(playerName, "player name");
        final Optional<PlayerInfo> optionalPlayerInfo = getPlayerInfo(playerName);
        if (!optionalPlayerInfo.isPresent()) {
            return false;
        }
        final PlayerInfo playerInfo = optionalPlayerInfo.get();

        if (playerInfo.getPlayerNames().remove(playerName)) {
            if (playerInfo.getPlayerNames().isEmpty()) {
                players.remove(playerInfo);
            }
            return true;
        } else {
            return false;
        }
    }

    public BindReceipt bind(long accountCode, String playerName) {
        if (!allowBind) {
            return BindReceipt.DENIED;
        }
        return forceBind(accountCode, playerName);
    }
}