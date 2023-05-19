package cn.chuanwise.xiaoming.minecraft.xiaoming.channel.trigger.xiaoming;

import cn.chuanwise.mclib.bukkit.net.Player;
import cn.chuanwise.common.util.Preconditions;
import cn.chuanwise.common.util.Strings;
import cn.chuanwise.xiaoming.minecraft.xiaoming.XMMCXiaoMingPlugin;
import cn.chuanwise.xiaoming.minecraft.xiaoming.channel.trigger.BindableTrigger;
import cn.chuanwise.xiaoming.minecraft.xiaoming.channel.trigger.TriggerHandleReceipt;
import cn.chuanwise.xiaoming.minecraft.xiaoming.configuration.PlayerInfo;
import cn.chuanwise.xiaoming.minecraft.xiaoming.net.OnlineClient;
import lombok.Data;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Data
public abstract class XiaoMingBindableTrigger<T>
        extends XiaoMingTrigger<T>
        implements BindableTrigger {

    /** 是否必须绑定才能激活触发器 */
    boolean mustBind = false;
    String accountTag;
    String xiaoMingPermission, playerPermission;

    @Override
    protected TriggerHandleReceipt handle0(T t) {
        final XMMCXiaoMingPlugin plugin = XMMCXiaoMingPlugin.getInstance();
        final long accountCode = getAccountCode(t);

        // 检查 AccountTag
        if (!Strings.isEmpty(accountTag)
                && !plugin.getXiaoMingBot().getAccountManager().hasTag(accountCode, accountTag)) {
            return TriggerHandleReceipt.Unhandled.getInstance();
        }

        // 检查绑定
        final Optional<PlayerInfo> optionalPlayerInfo = plugin.getPlayerConfiguration().getPlayerInfo(accountCode);
        if (!optionalPlayerInfo.isPresent() && mustBind) {
            return TriggerHandleReceipt.Unhandled.getInstance();
        }

        // 检查玩家权限
        if (!Strings.isEmpty(playerPermission)) {
            if (!optionalPlayerInfo.isPresent()) {
                return TriggerHandleReceipt.Unhandled.getInstance();
            }
            final PlayerInfo playerInfo = optionalPlayerInfo.get();

            boolean playerHasPermission = false;
            final List<OnlineClient> onlineClients = plugin.getServer().getOnlineClients();
            for (String playerName : playerInfo.getPlayerNames()) {
                for (OnlineClient onlineClient : onlineClients) {
                    try {
                        final Optional<Player> optionalPlayer = onlineClient.getRemoteContact().getPlayer(playerName);
                        if (optionalPlayer.isPresent()) {
                            final Player player = optionalPlayer.get();

                            if (player.hasPermission(playerPermission)) {
                                playerHasPermission = true;
                                break;
                            }
                        }
                    } catch (Exception exception) {
                        return TriggerHandleReceipt.Unhandled.getInstance();
                    }
                }
                if (playerHasPermission) {
                    break;
                }
            }

            if (!playerHasPermission) {
                return TriggerHandleReceipt.Unhandled.getInstance();
            }
        }

        // 检查小明权限
        if (!Strings.isEmpty(xiaoMingPermission)
                && !plugin.getXiaoMingBot().getPermissionService().hasPermission(accountCode, xiaoMingPermission)) {
            return TriggerHandleReceipt.Unhandled.getInstance();
        }

        // 调用下级接收器
        final TriggerHandleReceipt receipt = handle1(t);
        if (receipt instanceof TriggerHandleReceipt.Unhandled) {
            return receipt;
        }
        Preconditions.state(receipt instanceof TriggerHandleReceipt.Handled);
        final TriggerHandleReceipt.Handled handled = (TriggerHandleReceipt.Handled) receipt;

        // 扩充环境
        final Map<String, Object> environment = new HashMap<>(handled.getEnvironment());

        final String playerOrName;
        if (optionalPlayerInfo.isPresent()) {
            final PlayerInfo playerInfo = optionalPlayerInfo.get();
            environment.put("player", playerInfo);
            playerOrName = playerInfo.getPlayerNames().get(0);
        } else {
            playerOrName = plugin.getXiaoMingBot().getAccountManager().getAliasOrCode(accountCode);
        }
        environment.put("playerOrAlias", playerOrName);

        environment.put("accountTag", accountTag);
        environment.put("mustBind", mustBind);
        environment.put("xiaoMingPermission", xiaoMingPermission);
        environment.put("playerPermission", playerPermission);
        return new TriggerHandleReceipt.Handled(environment);
    }

    protected abstract TriggerHandleReceipt handle1(T t);

    protected abstract long getAccountCode(T t);
}
