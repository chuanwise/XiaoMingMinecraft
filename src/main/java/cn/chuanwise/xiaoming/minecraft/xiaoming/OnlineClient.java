package cn.chuanwise.xiaoming.minecraft.xiaoming;

import cn.chuanwise.mclib.net.contact.NetLibRemoteContact;
import cn.chuanwise.net.packet.NettyPacketService;
import cn.chuanwise.util.CollectionUtil;
import cn.chuanwise.util.ConditionUtil;
import cn.chuanwise.xiaoming.minecraft.protocol.SendWorldMessageRequest;
import cn.chuanwise.xiaoming.minecraft.protocol.XMMCProtocol;
import cn.chuanwise.xiaoming.minecraft.xiaoming.configuration.ServerInfo;
import cn.chuanwise.xiaoming.object.PluginObjectImpl;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import lombok.Getter;

import java.util.Set;

@Getter
public class OnlineClient extends PluginObjectImpl<Plugin> {
    protected final Channel channel;

    protected final Server server;
    protected final ServerInfo serverInfo;
    protected final NetLibRemoteContact remoteContact;

    protected final NettyPacketService packetService;

    public OnlineClient(Server server, Channel channel,  ServerInfo serverInfo) {
        ConditionUtil.notNull(server, "server");
        ConditionUtil.notNull(serverInfo, "server info");
        ConditionUtil.notNull(channel, "channel");

        this.server = server;
        this.serverInfo = serverInfo;
        this.channel = channel;

        this.packetService = new NettyPacketService();
        channel.pipeline().addLast(packetService);
        remoteContact = new NetLibRemoteContact(packetService);
    }

    public ChannelFuture disconnect() {
        return channel.close();
    }

    public void sendWorldMessage(Set<String> worldNames, Set<String> message) {
        worldNames = CollectionUtil.copyOf(worldNames);
        message = CollectionUtil.copyOf(message);
        packetService.inform(XMMCProtocol.INFORM_WORLD_MESSAGE, new SendWorldMessageRequest(worldNames, message));
    }
}
