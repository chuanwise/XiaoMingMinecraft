package cn.chuanwise.xiaoming.minecraft.xiaoming.event;

import cn.chuanwise.mclib.net.protocol.PlayerJoinInform;
import cn.chuanwise.xiaoming.minecraft.xiaoming.net.OnlineClient;
import lombok.Data;

import java.util.UUID;

@Data
public class PlayerJoinEvent
        extends SimpleXiaoMingMinecraftEvent
        implements PlayerEvent {

    private final UUID playerUuid;
    private final OnlineClient onlineClient;
    private final PlayerJoinInform inform;
    private final String playerName;
    private final String message;

    public PlayerJoinEvent(PlayerJoinInform inform, OnlineClient onlineClient) {
        this.inform = inform;

        this.playerUuid = inform.getPlayerUuid();
        this.playerName = inform.getPlayerName();
        this.message = inform.getMessage();

        this.onlineClient = onlineClient;
    }
}
