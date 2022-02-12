package cn.chuanwise.xiaoming.minecraft.xiaoming.event;

import cn.chuanwise.xiaoming.minecraft.xiaoming.net.OnlineClient;
import lombok.Data;

@Data
public class ServerTpsEvent
        extends SimpleXiaoMingMinecraftEvent
        implements MinecraftEvent {
    private final int tps;
    private final OnlineClient onlineClient;

    public ServerTpsEvent(int tps, OnlineClient onlineClient) {
        this.onlineClient = onlineClient;
        this.tps = tps;
    }
}
