package cn.chuanwise.xiaoming.minecraft.protocol;

import cn.chuanwise.net.packet.Packet;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class PacketInform {
    Packet packet;
}