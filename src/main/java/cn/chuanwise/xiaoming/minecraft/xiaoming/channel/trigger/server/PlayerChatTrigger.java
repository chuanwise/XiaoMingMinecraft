package cn.chuanwise.xiaoming.minecraft.xiaoming.channel.trigger.server;

import cn.chuanwise.common.util.Preconditions;
import cn.chuanwise.common.util.Strings;
import cn.chuanwise.xiaoming.account.Account;
import cn.chuanwise.xiaoming.minecraft.xiaoming.XMMCXiaoMingPlugin;
import cn.chuanwise.xiaoming.minecraft.xiaoming.channel.trigger.MessageFilter;
import cn.chuanwise.xiaoming.minecraft.xiaoming.channel.trigger.MessageFilterReceipt;
import cn.chuanwise.xiaoming.minecraft.xiaoming.channel.trigger.MessageFilterTrigger;
import cn.chuanwise.xiaoming.minecraft.xiaoming.channel.trigger.TriggerHandleReceipt;
import cn.chuanwise.xiaoming.minecraft.xiaoming.configuration.PlayerInfo;
import cn.chuanwise.xiaoming.minecraft.xiaoming.event.PlayerChatEvent;
import lombok.Data;
import net.mamoe.mirai.message.data.At;

import java.util.Map;

@Data
public class PlayerChatTrigger
        extends PlayerTrigger<PlayerChatEvent>
        implements MessageFilterTrigger {
    MessageFilter messageFilter = new MessageFilter.All();

    @Override
    protected TriggerHandleReceipt handle2(PlayerChatEvent inform) {
        final String message = inform.getMessage();
        final MessageFilterReceipt filterReceipt = messageFilter.filter(message);

        if (filterReceipt instanceof MessageFilterReceipt.Denied) {
            return TriggerHandleReceipt.Unhandled.getInstance();
        }
        Preconditions.state(filterReceipt instanceof MessageFilterReceipt.Accepted, "internal error");
        final MessageFilterReceipt.Accepted accepted = (MessageFilterReceipt.Accepted) filterReceipt;

        final StringBuilder stringBuilder = new StringBuilder(accepted.getMessage());

        // @Code，@Alias
        final XMMCXiaoMingPlugin plugin = XMMCXiaoMingPlugin.getInstance();
        for (Account account : plugin.getXiaoMingBot().getAccountManager().getAccounts().values()) {
            final String replaceTo = new At(account.getCode()).serializeToMiraiCode() + " ";

            // @code
            final String codeReplaceFrom = "@" + account.getCodeString();
            Strings.replaceAll(stringBuilder, codeReplaceFrom, replaceTo);

            // @alias
            if (Strings.nonEmpty(account.getAlias())) {
                final String aliasReplaceFrom = "@" + account.getAlias();
                Strings.replaceAll(stringBuilder, aliasReplaceFrom, replaceTo);
            }
        }

        // @PlayerName
        for (PlayerInfo playerInfo : plugin.getPlayerConfiguration().getPlayers()) {
            final String replaceTo = new At(playerInfo.getAccountCodes().iterator().next()).serializeToMiraiCode() + " ";

            for (String playerName : playerInfo.getPlayerNames()) {
                final String replaceFrom = "@" + playerName;
                Strings.replaceAll(stringBuilder, replaceFrom, replaceTo);
            }
        }

        final Map<String, Object> environment = accepted.getEnvironment();
        environment.put("message", stringBuilder.toString());
        return new TriggerHandleReceipt.Handled(environment);
    }
}
