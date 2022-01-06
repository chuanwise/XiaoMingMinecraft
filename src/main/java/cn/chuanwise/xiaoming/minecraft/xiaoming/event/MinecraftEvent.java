package cn.chuanwise.xiaoming.minecraft.xiaoming.event;

import cn.chuanwise.xiaoming.event.XiaomingEvent;
import cn.chuanwise.xiaoming.minecraft.xiaoming.net.OnlineClient;

public interface MinecraftEvent extends XiaomingEvent {
    OnlineClient getOnlineClient();
}
