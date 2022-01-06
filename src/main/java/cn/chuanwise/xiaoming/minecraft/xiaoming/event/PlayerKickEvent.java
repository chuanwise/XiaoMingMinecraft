package cn.chuanwise.xiaoming.minecraft.xiaoming.event;

import cn.chuanwise.mclib.net.protocol.PlayerKickInform;
import cn.chuanwise.xiaoming.minecraft.xiaoming.net.OnlineClient;
import lombok.Data;

import java.util.UUID;

@Data
public class PlayerKickEvent
        extends SimpleXiaoMingMinecraftCancellableEvent
        implements PlayerEvent {

    private final OnlineClient onlineClient;
    private final PlayerKickInform inform;
    private final UUID playerUuid;
    private final String playerName;
    private final String reason;

    public PlayerKickEvent(PlayerKickInform inform, OnlineClient onlineClient) {
        this.inform = inform;

        this.playerUuid = inform.getPlayerUuid();
        this.playerName = inform.getPlayerName();
        this.reason = inform.getReason();

        this.onlineClient = onlineClient;
    }
}
