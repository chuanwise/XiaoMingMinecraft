package cn.chuanwise.xiaoming.minecraft.xiaoming.channel.trigger.server;

import cn.chuanwise.mclib.net.protocol.PlayerInform;
import cn.chuanwise.util.ConditionUtil;
import cn.chuanwise.util.StringUtil;
import cn.chuanwise.xiaoming.minecraft.xiaoming.Plugin;
import cn.chuanwise.xiaoming.minecraft.xiaoming.channel.trigger.AccountTagTrigger;
import cn.chuanwise.xiaoming.minecraft.xiaoming.channel.trigger.TriggerHandleReceipt;
import cn.chuanwise.xiaoming.minecraft.xiaoming.configuration.PlayerConfiguration;
import cn.chuanwise.xiaoming.minecraft.xiaoming.configuration.PlayerInfo;
import lombok.Data;

import java.util.*;

@Data
public abstract class PlayerTrigger<T extends PlayerInform>
        extends ServerTrigger<T>
        implements AccountTagTrigger {
    String accountTag;

    @Override
    protected final TriggerHandleReceipt handle0(T t) {
        final Plugin plugin = Plugin.getInstance();
        final PlayerConfiguration playerConfiguration = plugin.getPlayerConfiguration();

        final boolean accountTagAllowed;
        if (StringUtil.isEmpty(accountTag)) {
            accountTagAllowed = true;
        } else {
            accountTagAllowed = playerConfiguration.getPlayerInfo(t.getPlayerName())
                    .map(PlayerInfo::getAccountCodes)
                    .map(x -> {
                        for (Long accountCode : x) {
                            if (plugin.getXiaomingBot()
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

        final TriggerHandleReceipt receipt = handle1(t);
        if (receipt instanceof TriggerHandleReceipt.Unhandled) {
            return receipt;
        }
        ConditionUtil.checkState(receipt instanceof TriggerHandleReceipt.Handled, "internal error");

        final Map<String, Object> environment = new HashMap<>();
        final String playerName = t.getPlayerName();

        environment.put("accountTag", accountTag);
        environment.put("player", playerName);
        environment.put("sender", playerName);
        environment.put("serverTag", serverTag);
        playerConfiguration.getPlayerInfo(playerName)
                .ifPresent(x -> {
                    environment.put("player", x);
                    environment.put("sender", x);
                });

        environment.putAll(((TriggerHandleReceipt.Handled) receipt).getEnvironment());
        return new TriggerHandleReceipt.Handled(environment, messages);
    }

    protected TriggerHandleReceipt handle1(T t) {
        return new TriggerHandleReceipt.Handled(Collections.emptyMap(), messages);
    }

    protected boolean canHandle1(T t) {
        return true;
    }

    @Override
    public final String getDescription() {
        if (StringUtil.notEmpty(accountTag)) {
            return "服务器 #" + serverTag + " 用户 #" + accountTag + " 的" + getDescription1();
        } else {
            return "服务器 #" + serverTag + " 所有用户的" + getDescription1();
        }
    }

    protected abstract String getDescription1();

    protected Map<String, Object> buildEnvironment1(T t) {
        return Collections.emptyMap();
    }
}