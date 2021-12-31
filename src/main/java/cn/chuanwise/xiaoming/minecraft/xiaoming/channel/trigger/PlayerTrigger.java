package cn.chuanwise.xiaoming.minecraft.xiaoming.channel.trigger;

import lombok.Data;
import org.bukkit.event.player.PlayerEvent;

import java.util.HashSet;
import java.util.Set;

@Data
public abstract class PlayerTrigger extends ServerTrigger {
    protected final Set<String> playerNames = new HashSet<>();

    protected final Set<Long> playerCodes = new HashSet<>();
}
