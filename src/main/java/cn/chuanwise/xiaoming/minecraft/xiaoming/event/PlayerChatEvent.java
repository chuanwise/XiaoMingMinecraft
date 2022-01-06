package cn.chuanwise.xiaoming.minecraft.xiaoming.event;

import cn.chuanwise.mclib.net.protocol.PlayerChatInform;
import cn.chuanwise.xiaoming.minecraft.xiaoming.net.OnlineClient;
import lombok.Data;

import java.util.UUID;

@Data
public class PlayerChatEvent
        extends SimpleXiaoMingMinecraftCancellableEvent
        implements PlayerEvent {
    private final String playerName;
    private final UUID playerUuid;
    private final String message;
    private final OnlineClient onlineClient;

    private final PlayerChatInform inform;

    public PlayerChatEvent(PlayerChatInform inform, OnlineClient onlineClient) {
        this.inform = inform;

        this.playerName = inform.getPlayerName();
        this.playerUuid = inform.getPlayerUuid();
        this.message = inform.getMessage();

        this.onlineClient = onlineClient;
    }
}
