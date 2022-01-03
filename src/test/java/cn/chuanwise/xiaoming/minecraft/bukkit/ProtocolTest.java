package cn.chuanwise.xiaoming.minecraft.bukkit;

import cn.chuanwise.mclib.net.protocol.NetLibProtocol;
import cn.chuanwise.xiaoming.minecraft.protocol.XMMCProtocol;
import org.junit.jupiter.api.Test;

public class ProtocolTest {
    @Test
    void testProtocol() {
        System.out.println(XMMCProtocol.getInstance().getPacketTypes());
    }

    @Test
    void testListener() {
        final NetLibProtocol protocol = NetLibProtocol.getInstance();
        System.out.println(protocol.getPacketTypes());
    }
}
