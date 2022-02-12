package cn.chuanwise.xiaoming.minecraft.xiaoming.event;

import cn.chuanwise.xiaoming.event.XiaoMingEvent;
import cn.chuanwise.xiaoming.minecraft.xiaoming.net.OnlineClient;

public interface MinecraftEvent extends XiaoMingEvent {
    OnlineClient getOnlineClient();
}
