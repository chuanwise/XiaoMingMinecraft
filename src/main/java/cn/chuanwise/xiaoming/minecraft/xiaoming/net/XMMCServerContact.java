package cn.chuanwise.xiaoming.minecraft.xiaoming.net;

import cn.chuanwise.mclib.net.protocol.NetLibProtocol;
import cn.chuanwise.net.packet.NettyPacketService;
import cn.chuanwise.util.ConditionUtil;
import cn.chuanwise.xiaoming.annotation.EventListener;
import cn.chuanwise.xiaoming.event.Listeners;
import cn.chuanwise.xiaoming.event.MessageEvent;
import cn.chuanwise.xiaoming.minecraft.xiaoming.Plugin;
import cn.chuanwise.xiaoming.minecraft.xiaoming.channel.Channel;
import cn.chuanwise.xiaoming.minecraft.xiaoming.configuration.ChannelConfiguration;
import cn.chuanwise.xiaoming.object.PluginObjectImpl;
import lombok.Data;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Data
@SuppressWarnings("all")
public class XMMCServerContact extends PluginObjectImpl<Plugin> implements Listeners<Plugin> {
    protected final NettyPacketService packetService;

    public XMMCServerContact(Plugin plugin, NettyPacketService packetService) {
        setPlugin(plugin);
        ConditionUtil.notNull(packetService, "packet service");
        this.packetService = packetService;
    }

    @EventListener
    private void onMessageEvent(MessageEvent event) {
        fireTriggerHandle(event);
    }

    private void fireTriggerHandle(Object object) {
        final ChannelConfiguration channelConfiguration = plugin.getChannelConfiguration();

        final Map<String, Channel> channels = channelConfiguration.getChannels();
        for (Channel channel : channels.values()) {
            final List<String> messages = channel.getTriggers()
                    .stream()
                    .map(x -> x.handle(object))
                    .flatMap(Collection::stream)
                    .collect(Collectors.toUnmodifiableList());

            if (messages.isEmpty()) {
                continue;
            }

            channel.getScopes().forEach(x -> x.sendMessage(plugin, messages));
        }
    }

    private void initTriggerListeners() {
        packetService.setOnPacket(NetLibProtocol.INFORM_PLAYER_CHANGE_WORLD, this::fireTriggerHandle);
        packetService.setOnPacket(NetLibProtocol.INFORM_PLAYER_CHAT, this::fireTriggerHandle);
        packetService.setOnPacket(NetLibProtocol.INFORM_PLAYER_JOIN, this::fireTriggerHandle);
        packetService.setOnPacket(NetLibProtocol.INFORM_PLAYER_QUIT, this::fireTriggerHandle);
        packetService.setOnPacket(NetLibProtocol.INFORM_PLAYER_DEATH, this::fireTriggerHandle);
        packetService.setOnPacket(NetLibProtocol.INFORM_PLAYER_KICK, this::fireTriggerHandle);
    }
}
