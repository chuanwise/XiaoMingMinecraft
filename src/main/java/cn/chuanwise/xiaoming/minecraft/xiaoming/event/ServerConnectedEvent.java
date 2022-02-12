package cn.chuanwise.xiaoming.minecraft.xiaoming.event;

import cn.chuanwise.xiaoming.minecraft.xiaoming.net.OnlineClient;
import lombok.Data;

@Data
public class ServerConnectedEvent extends SimpleXiaoMingMinecraftCancellableEvent {
    protected final OnlineClient onlineClient;
}
