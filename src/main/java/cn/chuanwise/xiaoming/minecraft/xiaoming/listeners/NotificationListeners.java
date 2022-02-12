package cn.chuanwise.xiaoming.minecraft.xiaoming.listeners;

import cn.chuanwise.util.Collections;
import cn.chuanwise.util.Strings;
import cn.chuanwise.xiaoming.account.Account;
import cn.chuanwise.xiaoming.annotation.EventListener;
import cn.chuanwise.xiaoming.contact.contact.XiaoMingContact;
import cn.chuanwise.xiaoming.event.MessageEvent;
import cn.chuanwise.xiaoming.event.SimpleListeners;
import cn.chuanwise.xiaoming.minecraft.xiaoming.XMMCXiaoMingPlugin;
import cn.chuanwise.xiaoming.minecraft.xiaoming.configuration.NotificationConfiguration;
import cn.chuanwise.xiaoming.minecraft.xiaoming.configuration.PlayerInfo;
import cn.chuanwise.xiaoming.minecraft.xiaoming.net.OnlineClient;
import cn.chuanwise.xiaoming.user.GroupXiaoMingUser;
import cn.chuanwise.xiaoming.util.MiraiCodes;
import lombok.Data;
import net.mamoe.mirai.message.data.*;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class NotificationListeners extends SimpleListeners<XMMCXiaoMingPlugin> {
    private final Pattern AT_PATTERN = Pattern.compile("\\[mirai:at:(?<target>\\d+)\\]\\s*");

    @Data
    private static class ReplaceAt {
        final long accountCode;
    }

    @Override
    public void onRegister() {
        xiaoMingBot.getLanguageManager().registerConvertor(ReplaceAt.class, x -> xiaoMingBot.getAccountManager().getAliasOrCode(x.accountCode), plugin);
        xiaoMingBot.getLanguageManager().registerOperators(ReplaceAt.class, plugin)
                .addOperator("code", ReplaceAt::getAccountCode)
                .addOperator("accountCode", ReplaceAt::getAccountCode)
                .addOperator("alias", x -> xiaoMingBot.getAccountManager().getAliasOrCode(x.accountCode))
                .addOperator("player", x -> plugin.getPlayerConfiguration().getPlayerInfo(x.accountCode).orElse(null))
                .addOperator("playerOrName", x -> plugin.getPlayerConfiguration().getPlayerInfo(x.accountCode).map(y -> y.getPlayerNames().get(0)).orElse(xiaoMingBot.getAccountManager().getAliasOrCode(x.accountCode)))
                .addOperator("playerOrAlias", x -> plugin.getPlayerConfiguration().getPlayerInfo(x.accountCode).map(y -> y.getPlayerNames().get(0)).orElse(xiaoMingBot.getAccountManager().getAliasOrCode(x.accountCode)));
    }

    @EventListener
    void onMessageEvent(MessageEvent event) {
        xiaoMingBot.getScheduler().run(() -> {
            onMessageEventSync(event);
        });
    }

    void onMessageEventSync(MessageEvent event) {
        final String message = event.getMessage().serialize();
        if (!(event.getUser() instanceof GroupXiaoMingUser)) {
            return;
        }

        final NotificationConfiguration notificationConfiguration = plugin.getNotificationConfiguration();
        final GroupXiaoMingUser user = (GroupXiaoMingUser) event.getUser();

        // check if at all
        boolean hasAtAll = message.contains(AtAll.INSTANCE.serializeToMiraiCode());

        // collect at
        // replace at
        final NotificationConfiguration.NotificationInfo at = plugin.getNotificationConfiguration().getAt();
        final Map<String, Object> replaceEnvironment = new HashMap<>();
        final Set<Long> atAccountCodes = new HashSet<>();

        final StringBuilder messageStringBuilder = new StringBuilder(message);

        // replace @AccountCode, @Alias
        for (Account account : plugin.getXiaoMingBot().getAccountManager().getAccounts().values()) {
            replaceEnvironment.put("target", new ReplaceAt(account.getCode()));
            final String replaceTo = user.formatAdditional(at.getPlaceHolder(), replaceEnvironment::get);

            // @AccountCode
            final String accountCodeReplaceFrom = "@" + account.getCodeString();
            final boolean accountCodeReplaced = Strings.replaceAll(messageStringBuilder, accountCodeReplaceFrom, replaceTo) > 0;

            // @Alias
            final boolean aliasReplaced;
            if (!Strings.isEmpty(account.getAlias())) {
                final String aliasReplaceFrom = "@" + account.getAlias();
                aliasReplaced = Strings.replaceAll(messageStringBuilder, aliasReplaceFrom, replaceTo) > 0;
            } else {
                aliasReplaced = false;
            }

            if (accountCodeReplaced || aliasReplaced) {
                atAccountCodes.add(account.getCode());
            }
        }

        // replace actual at
        Matcher atMatcher = AT_PATTERN.matcher(messageStringBuilder);
        int position = 0;
        while (atMatcher.find(position)) {
            final int start = atMatcher.start();
            final int end = atMatcher.end();
            position = start + 1;

            final long target = Long.parseLong(atMatcher.group("target"));
            atAccountCodes.add(target);

            replaceEnvironment.put("target", new ReplaceAt(target));
            final String replaceTo = user.formatAdditional(at.getPlaceHolder(), replaceEnvironment::get);
            messageStringBuilder.replace(start, end, replaceTo);

            atMatcher = AT_PATTERN.matcher(messageStringBuilder);
        }

        final Set<String> atPlayerNames = new HashSet<>();

        // replace player names
        for (PlayerInfo playerInfo : plugin.getPlayerConfiguration().getPlayers()) {
            final List<String> playerNames = playerInfo.getPlayerNames();
            boolean thisPlayerAt = false;
            for (String playerName : playerNames) {
                replaceEnvironment.put("target", new ReplaceAt(playerInfo.getAccountCodes().iterator().next()));

                // replace @PlayerName
                final String replaceFrom = "@" + playerName;
                final String replaceTo = user.formatAdditional(at.getPlaceHolder(), replaceEnvironment::get);

                final boolean replaced = Strings.replaceAll(messageStringBuilder, replaceFrom, replaceTo) > 0;
                if (replaced) {
                    thisPlayerAt = true;
                }
            }

            // verify if at
            thisPlayerAt = thisPlayerAt || Collections.containsAny(atAccountCodes, playerInfo.getAccountCodes());

            if (thisPlayerAt) {
                atPlayerNames.addAll(playerNames);
            }
        }

        final String replacedMessage = messageStringBuilder.toString();
        final Map<String, Object> environment = new HashMap<>();
        environment.put("message", MiraiCodes.contentToString(replacedMessage));

        // 转发 AtAll
        if (hasAtAll) {
            final List<OnlineClient> onlineClients = plugin.getServer().getOnlineClients();
            if (!onlineClients.isEmpty()) {
                final NotificationConfiguration.NotificationInfo atAll = notificationConfiguration.getAtAll();

                // 获取群友绑定的所有玩家名
                final Set<String> playerNames = user.getContact()
                        .getMembers()
                        .stream()
                        .map(XiaoMingContact::getCode)
                        .map(plugin.getPlayerConfiguration()::getPlayerInfo)
                        .filter(Optional::isPresent)
                        .map(Optional::get)
                        .map(PlayerInfo::getPlayerNames)
                        .flatMap(Collection::stream)
                        .collect(Collectors.toSet());

                // 发送数据包
                for (OnlineClient onlineClient : onlineClients) {
                    environment.put("server", onlineClient);

                    final NotificationConfiguration.NotificationInfo.Title atAllTitle = atAll.getTitle();
                    if (atAllTitle.isEnable()) {
                        final String finalTitle = user.formatAdditional(atAllTitle.getTitle(), environment::get);
                        final String finalSubTitle = user.formatAdditional(atAllTitle.getSubTitle(), environment::get);

                        onlineClient.getRemoteContact().broadcastTitle(playerNames,
                                finalTitle, finalSubTitle, atAllTitle.getFadeIn(), atAllTitle.getDelay(), atAllTitle.getFadeOut());
                    }

                    final NotificationConfiguration.NotificationInfo.Message atAllMessage = atAll.getMessage();
                    if (atAllMessage.isEnable()) {
                        final String finalMessage = user.formatAdditional(atAllMessage.getMessage(), environment::get);
                        onlineClient.getServerClient().sendWideMessage(playerNames, finalMessage);
                    }
                }
            }
        }

        // 转发 At
        if (!atPlayerNames.isEmpty())  {
            final List<OnlineClient> onlineClients = plugin.getServer().getOnlineClients();
            if (!onlineClients.isEmpty()) {
                for (OnlineClient onlineClient : onlineClients) {
                    environment.put("server", onlineClient);

                    final NotificationConfiguration.NotificationInfo.Title atAllTitle = at.getTitle();
                    if (atAllTitle.isEnable()) {
                        final String finalTitle = user.formatAdditional(atAllTitle.getTitle(), environment::get);
                        final String finalSubTitle = user.formatAdditional(atAllTitle.getSubTitle(), environment::get);

                        onlineClient.getRemoteContact().broadcastTitle(atPlayerNames,
                                finalTitle, finalSubTitle, atAllTitle.getFadeIn(), atAllTitle.getDelay(), atAllTitle.getFadeOut());
                    }

                    final NotificationConfiguration.NotificationInfo.Message atAllMessage = at.getMessage();
                    if (atAllMessage.isEnable()) {
                        final String finalMessage = user.formatAdditional(atAllMessage.getMessage(), environment::get);
                        onlineClient.getServerClient().sendWideMessage(atPlayerNames, finalMessage);
                    }
                }
            }
        }
    }
}
