package cn.chuanwise.xiaoming.minecraft.xiaoming.event;

import cn.chuanwise.mclib.bukkit.net.protocol.PlayerQuitInform;
import cn.chuanwise.xiaoming.minecraft.xiaoming.net.OnlineClient;
import lombok.Data;

import java.util.UUID;

@Data
public class PlayerQuitEvent
        extends SimpleXiaoMingMinecraftEvent
        implements PlayerEvent {

    private final OnlineClient onlineClient;
    private final PlayerQuitInform inform;
    private final String playerName;
    private final UUID playerUuid;
    private final String message;

    public PlayerQuitEvent(PlayerQuitInform inform, OnlineClient onlineClient) {
        this.inform = inform;

        this.playerName = inform.getPlayerName();
        this.playerUuid = inform.getPlayerUuid();
        this.message = inform.getMessage();

        this.onlineClient = onlineClient;
    }
}
