package cn.chuanwise.xiaoming.minecraft.xiaoming.event;

import cn.chuanwise.mclib.net.protocol.PlayerChangeWorldInform;
import cn.chuanwise.mclib.net.protocol.PlayerJoinInform;
import cn.chuanwise.xiaoming.minecraft.xiaoming.net.OnlineClient;
import lombok.Data;

import java.util.UUID;

@Data
public class PlayerChangeWorldEvent
        extends SimpleXiaoMingMinecraftEvent
        implements PlayerEvent {

    private final UUID playerUuid;
    private final OnlineClient onlineClient;
    private final String playerName;
    private final PlayerChangeWorldInform inform;
    private final String fromWorldName;
    private final String toWorldName;
    private final UUID fromWorldUuid;
    private final UUID toWorldUuid;

    public PlayerChangeWorldEvent(PlayerChangeWorldInform inform, OnlineClient onlineClient) {
        this.inform = inform;

        this.playerUuid = inform.getPlayerUuid();
        this.playerName = inform.getPlayerName();

        this.fromWorldName = inform.getFromWorldName();
        this.toWorldName = inform.getToWorldName();

        this.fromWorldUuid = inform.getFromWorldUuid();
        this.toWorldUuid = inform.getToWorldUuid();

        this.onlineClient = onlineClient;
    }
}
