package cn.chuanwise.xiaoming.minecraft.xiaoming.channel.trigger.server;

import cn.chuanwise.mclib.bukkit.net.Player;
import cn.chuanwise.common.util.Preconditions;
import cn.chuanwise.common.util.Strings;
import cn.chuanwise.xiaoming.minecraft.xiaoming.XMMCXiaoMingPlugin;
import cn.chuanwise.xiaoming.minecraft.xiaoming.channel.trigger.*;
import cn.chuanwise.xiaoming.minecraft.xiaoming.configuration.PlayerConfiguration;
import cn.chuanwise.xiaoming.minecraft.xiaoming.configuration.PlayerInfo;
import cn.chuanwise.xiaoming.minecraft.xiaoming.event.PlayerEvent;
import lombok.Data;

import java.util.*;

@Data
public abstract class PlayerTrigger<T extends PlayerEvent>
        extends ServerTrigger<T>
        implements BindableTrigger {
    boolean mustBind;
    String accountTag;
    String xiaoMingPermission, playerPermission;

    @Override
    protected final TriggerHandleReceipt handle1(T t) {
        final XMMCXiaoMingPlugin plugin = XMMCXiaoMingPlugin.getInstance();
        final PlayerConfiguration playerConfiguration = plugin.getPlayerConfiguration();

        // 检查 AccountTag
        final boolean accountTagAllowed;
        if (Strings.isEmpty(accountTag)) {
            accountTagAllowed = true;
        } else {
            accountTagAllowed = playerConfiguration.getPlayerInfo(t.getPlayerName())
                    .map(PlayerInfo::getAccountCodes)
                    .map(x -> {
                        for (Long accountCode : x) {
                            if (plugin.getXiaoMingBot()
                                    .getAccountManager()
                                    .getTags(accountCode)
                                    .contains(accountTag)) {
                                return true;
                            }
                        }
                        return false;
                    })
                    .orElse(false);
        }
        if (!accountTagAllowed) {
            return TriggerHandleReceipt.Unhandled.getInstance();
        }

        // 检查绑定
        final Optional<PlayerInfo> optionalPlayerInfo = playerConfiguration.getPlayerInfo(t.getPlayerName());
        if (!optionalPlayerInfo.isPresent() && mustBind) {
            return TriggerHandleReceipt.Unhandled.getInstance();
        }

        // 检查 XiaoMingPermission
        if (!Strings.isEmpty(xiaoMingPermission)) {
            if (!optionalPlayerInfo.isPresent()) {
                return TriggerHandleReceipt.Unhandled.getInstance();
            }
            final PlayerInfo playerInfo = optionalPlayerInfo.get();

            boolean hasPermission = false;
            for (Long accountCode : playerInfo.getAccountCodes()) {
                if (plugin.getXiaoMingBot().getPermissionService().hasPermission(accountCode, xiaoMingPermission)) {
                    hasPermission = true;
                    break;
                }
            }

            if (!hasPermission) {
                return TriggerHandleReceipt.Unhandled.getInstance();
            }
        }

        // 检查 PlayerPermission
        if (!Strings.isEmpty(playerPermission)) {
            try {
                final Optional<Player> optionalPlayer = t.getOnlineClient().getRemoteContact().getPlayer(t.getPlayerUuid());
                if (!optionalPlayer.isPresent()) {
                    return TriggerHandleReceipt.Unhandled.getInstance();
                }
                final Player player = optionalPlayer.get();
                if (!player.hasPermission(playerPermission)) {
                    return TriggerHandleReceipt.Unhandled.getInstance();
                }
            } catch (Exception exception) {
                return TriggerHandleReceipt.Unhandled.getInstance();
            }
        }

        final TriggerHandleReceipt receipt = handle2(t);
        if (receipt instanceof TriggerHandleReceipt.Unhandled) {
            return receipt;
        }
        Preconditions.state(receipt instanceof TriggerHandleReceipt.Handled);
        final TriggerHandleReceipt.Handled handled = (TriggerHandleReceipt.Handled) receipt;

        final Map<String, Object> environment = new HashMap<>(handled.getEnvironment());
        final String playerName = t.getPlayerName();

        environment.put("mustBind", mustBind);
        environment.put("accountTag", accountTag);
        environment.put("xiaoMingPermission", xiaoMingPermission);
        environment.put("playerPermission", playerPermission);

        // player name or alias
        final Object playerObject;
        final String nameOrAlias;
        if (optionalPlayerInfo.isPresent()) {
            final PlayerInfo playerInfo = optionalPlayerInfo.get();
            playerObject = playerInfo;
            final long accountCode = playerInfo.getAccountCodes().iterator().next();
            nameOrAlias = plugin.getXiaoMingBot().getAccountManager().getAliasOrCode(accountCode);
        } else {
            playerObject = playerName;
            nameOrAlias = playerName;
        }
        environment.put("player", playerObject);
        environment.put("sender", playerObject);
        environment.put("playerOrAlias", nameOrAlias);

        return new TriggerHandleReceipt.Handled(environment);
    }

    protected abstract TriggerHandleReceipt handle2(T t);
}