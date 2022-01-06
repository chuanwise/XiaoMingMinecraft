package cn.chuanwise.xiaoming.minecraft.xiaoming.event;

import java.util.UUID;

public interface PlayerEvent extends MinecraftEvent {
    String getPlayerName();

    UUID getPlayerUuid();
}
