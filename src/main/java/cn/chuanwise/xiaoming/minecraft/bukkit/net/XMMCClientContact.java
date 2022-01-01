package cn.chuanwise.xiaoming.minecraft.bukkit.net;

import cn.chuanwise.mclib.bukkit.BukkitPluginObject;
import cn.chuanwise.net.packet.NettyPacketService;
import cn.chuanwise.util.ConditionUtil;
import cn.chuanwise.xiaoming.minecraft.bukkit.Plugin;
import lombok.Getter;
import org.bukkit.event.Listener;

@Getter
public class XMMCClientContact extends BukkitPluginObject<Plugin> implements Listener {
    protected final NettyPacketService packetService;

    public XMMCClientContact(Plugin plugin, NettyPacketService packetService) {
        super(plugin);
        ConditionUtil.notNull(packetService, "packet service");
        this.packetService = packetService;
    }
}
