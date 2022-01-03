package cn.chuanwise.xiaoming.minecraft.protocol;

import cn.chuanwise.mclib.net.Player;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class OnlinePlayerResponse {
    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class PlayerKey {
        UUID playerUuid;
        String playerName;
        String playerDisplayName;
        String playerListName;

        UUID worldUuid;
        String worldName;
    }
    Set<PlayerKey> playerKeys = new HashSet<>();
}
