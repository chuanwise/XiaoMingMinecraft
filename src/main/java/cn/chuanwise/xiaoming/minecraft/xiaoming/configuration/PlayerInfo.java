package cn.chuanwise.xiaoming.minecraft.xiaoming.configuration;

import lombok.Data;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Data
public class PlayerInfo {
    Set<Long> accountCodes = new HashSet<>();
    List<String> playerNames = new ArrayList<>();

    public boolean hasPlayerName(String playerName) {
        return playerNames.contains(playerName);
    }

    public boolean hasAccountCode(long accountCode) {
        return accountCodes.contains(accountCode);
    }
}
