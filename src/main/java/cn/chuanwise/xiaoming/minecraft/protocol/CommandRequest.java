package cn.chuanwise.xiaoming.minecraft.protocol;

import cn.chuanwise.mclib.net.protocol.SimplePlayerRelated;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@NoArgsConstructor
public class CommandRequest extends SimplePlayerRelated {
    String command;
    long timeout;

    public CommandRequest(UUID playerUuid, String playerName, String command, long timeout) {
        super(playerUuid, playerName);
        this.command = command;
        this.timeout = timeout;
    }
}
