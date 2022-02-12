package cn.chuanwise.xiaoming.minecraft.xiaoming.channel.executor.xiaoming;

import cn.chuanwise.xiaoming.account.Account;
import cn.chuanwise.xiaoming.minecraft.xiaoming.XMMCXiaoMingPlugin;
import cn.chuanwise.xiaoming.minecraft.xiaoming.channel.executor.AccountTagExecutor;
import cn.chuanwise.xiaoming.minecraft.xiaoming.channel.executor.FormatExecutor;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public class PrivateBroadcastExecutor
        extends XiaoMingExecutor
        implements AccountTagExecutor, FormatExecutor {
    String accountTag;
    String format;

    @Override
    public void execute(Map<String, Object> environment) {
        environment.put("accountTag", accountTag);
        environment.put("format", format);

        // translate
        final XMMCXiaoMingPlugin plugin = XMMCXiaoMingPlugin.getInstance();
        final String finalMessage = plugin.getXiaoMingBot().getLanguageManager().formatAdditional(format, environment::get);

        // send
        final List<Account> accounts = plugin.getXiaoMingBot().getAccountManager().searchAccountsByTag(accountTag);
        for (Account account : accounts) {
            plugin.getXiaoMingBot().getContactManager().sendPrivateMessagePossibly(account.getCode(), finalMessage);
        }
    }
}
