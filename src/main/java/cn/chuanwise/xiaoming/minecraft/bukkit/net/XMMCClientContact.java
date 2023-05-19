package cn.chuanwise.xiaoming.minecraft.bukkit.net;

import cn.chuanwise.mclib.bukkit.ask.Asker;
import cn.chuanwise.mclib.bukkit.event.BukkitTpsEvent;
import cn.chuanwise.mclib.bukkit.plugin.BukkitPluginObject;
import cn.chuanwise.mclib.bukkit.util.Players;
import cn.chuanwise.net.netty.packet.PacketHandler;
import cn.chuanwise.common.util.Preconditions;
import cn.chuanwise.common.util.Times;
import cn.chuanwise.xiaoming.minecraft.bukkit.XMMCBukkitPlugin;
import cn.chuanwise.xiaoming.minecraft.bukkit.event.XiaoMingMessageEvent;
import cn.chuanwise.xiaoming.minecraft.protocol.*;
import lombok.Getter;
import org.bukkit.ChatColor;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

@Getter
public class XMMCClientContact
        extends BukkitPluginObject<XMMCBukkitPlugin>
        implements Listener {
    protected final PacketHandler packetHandler;

    public XMMCClientContact(XMMCBukkitPlugin plugin, PacketHandler packetHandler) {
        super(plugin);
        Preconditions.nonNull(packetHandler, "packet handler");
        this.packetHandler = packetHandler;

        setupBaseListeners();
        setupMessageListeners();
    }

    public Optional<PlayerBindInfo> requestBindInfo(String playerName) throws InterruptedException, TimeoutException {
        return Optional.ofNullable(packetHandler.request(XMMCProtocol.REQUEST_PLAYER_BIND_INFO, playerName));
    }

    public Optional<PlayerVerifyCodeInfo> requestAllocatePlayerVerifyCode(String playerName) throws InterruptedException, TimeoutException {
        return Optional.ofNullable(packetHandler.request(XMMCProtocol.REQUEST_PLAYER_VERIFY_CODE, playerName));
    }

    private void setupBaseListeners() {
        packetHandler.setOnRequest(XMMCProtocol.REQUEST_CONFIRM_ACTIVE, v -> v);
        packetHandler.setOnInform(XMMCProtocol.INFORM_MESSAGE, argument -> {
            plugin.getServer().getPluginManager().callEvent(new XiaoMingMessageEvent(argument));
        });
        packetHandler.setOnInform(XMMCProtocol.INFORM_WIDE_MESSAGE, argument -> {
            for (Player onlinePlayer : plugin.getServer().getOnlinePlayers()) {
                if (argument.getPlayerNames().contains(onlinePlayer.getName())) {
                    onlinePlayer.sendMessage(argument.getMessage());
                }
            }
        });
    }

    private void setupMessageListeners() {
        packetHandler.setOnInform(XMMCProtocol.INFORM_WORLD_MESSAGE, inform -> {
            final Set<String> worldNames = inform.getWorldNames();
            final List<String> messages = inform.getMessages();

            for (World world : server().getWorlds()) {
                if (worldNames.contains(world.getName())) {
                    world.getPlayers().forEach(x -> messages.forEach(x::sendMessage));
                }
            }
        });

        packetHandler.setOnObtain(XMMCProtocol.OBTAIN_ONLINE_PLAYERS, () -> {
            return new OnlinePlayerResponse(server().getOnlinePlayers()
                    .stream()
                    .map(x -> new OnlinePlayerResponse.PlayerKey(
                            x.getUniqueId(), x.getName(),
                            x.getDisplayName(), x.getPlayerListName(),
                            x.getWorld().getUID(), x.getWorld().getName()
                    ))
                    .collect(Collectors.toSet()));
        });

        packetHandler.setOnRequest(XMMCProtocol.REQUEST_ASK, request -> {
            final String playerName = request.getPlayerName();
            final Optional<Player> optionalPlayer = Players.getPlayer(server(), playerName);
            if (!optionalPlayer.isPresent()) {
                return AskResponse.OFFLINE;
            }
            final Player player = optionalPlayer.get();

            final Optional<Asker> optionalAsker = plugin.getAskerManager().ask(player);
            if (!optionalAsker.isPresent()) {
                return AskResponse.CONFLICT;
            }
            final Asker asker = optionalAsker.get();

            final long timeout = request.getTimeout();
            final String message = request.getMessage();

            // 显示信息
            communicator().infoString(player, message);
            player.sendMessage(ChatColor.WHITE + "接受请求，输入 " + ChatColor.GRAY + "/" + ChatColor.GREEN + "xmaccept");
            player.sendMessage(ChatColor.WHITE + "拒绝请求，输入 " + ChatColor.GRAY + "/" + ChatColor.YELLOW + "xmdeny");
            player.sendMessage(ChatColor.WHITE + "忽略请求，输入 " + ChatColor.GRAY + "/" + ChatColor.YELLOW + "xmignore");
            player.sendMessage(ChatColor.GRAY + "此请求将在 " + ChatColor.AQUA + Times.toTimeLength(timeout) + ChatColor.GRAY + " 后自动取消");

            // 等待消息
            switch (asker.get(timeout)) {
                case TIMEOUT:
                    return AskResponse.TIMEOUT;
                case DENIED:
                    return AskResponse.DENIED;
                case IGNORED:
                    return AskResponse.IGNORED;
                case ACCEPTED:
                    return AskResponse.ACCEPTED;
                default:
                    throw new NoSuchElementException();
            }
        });
    }

    public PlayerBindResponse requestBind(String playerName, long accountCode) throws InterruptedException, TimeoutException {
        return packetHandler.request(XMMCProtocol.REQUEST_PLAYER_BIND, new PlayerBindRequest(playerName, accountCode));
    }

    public boolean requestUnbind(String playerName) throws InterruptedException, TimeoutException {
        return packetHandler.request(XMMCProtocol.REQUEST_PLAYER_UNBIND, playerName);
    }

    public boolean confirmActive() throws InterruptedException {
        boolean active;
        try {
            final long timeMillis = System.currentTimeMillis();
            active = timeMillis == packetHandler.request(XMMCProtocol.REQUEST_CONFIRM_ACTIVE, timeMillis);
        } catch (TimeoutException e) {
            active = false;
        }
        if (!active) {
            packetHandler.getContext().channel().close();
        }
        return active;
    }

    public void sendMessage(String message) {
        packetHandler.inform(XMMCProtocol.INFORM_MESSAGE, message);
    }

    @EventHandler
    protected void onTpsEvent(BukkitTpsEvent event) {
        packetHandler.inform(XMMCProtocol.INFORM_TPS, event.getTps());
    }
}
