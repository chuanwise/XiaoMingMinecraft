package cn.chuanwise.xiaoming.minecraft.xiaoming.configuration;

import cn.chuanwise.xiaoming.minecraft.xiaoming.Plugin;
import cn.chuanwise.xiaoming.minecraft.xiaoming.channel.Channel;
import cn.chuanwise.xiaoming.minecraft.xiaoming.channel.scope.QQScope;
import cn.chuanwise.xiaoming.minecraft.xiaoming.channel.scope.ServerScope;
import cn.chuanwise.xiaoming.minecraft.xiaoming.channel.trigger.Trigger;
import cn.chuanwise.xiaoming.minecraft.xiaoming.channel.trigger.TriggerHandleReceipt;
import cn.chuanwise.xiaoming.minecraft.xiaoming.channel.trigger.server.ServerTrigger;
import cn.chuanwise.xiaoming.minecraft.xiaoming.channel.trigger.xiaoming.QQTrigger;
import cn.chuanwise.xiaoming.minecraft.xiaoming.net.OnlineClient;
import cn.chuanwise.xiaoming.preservable.SimplePreservable;
import lombok.Data;

import java.util.*;
import java.util.stream.Collectors;

@Data
@SuppressWarnings("all")
public class ChannelConfiguration extends SimplePreservable<Plugin> {
    Map<String, Channel> channels = new HashMap<>();

    @Data
    public static class DefaultValue {
        String groupMessageTriggerMessage = "§7[§3{channel}§7] §b{sender} §8§l: §f{message}";
        String playerChatTriggerMessage = "{player}：{message}";
    }
    DefaultValue defaultValue = new DefaultValue();

    public Set<ChannelHandlerReceipt> triggerHandle(Object object, Class<? extends Trigger> triggerClass) {
        final ChannelConfiguration channelConfiguration = plugin.getChannelConfiguration();

        final Set<ChannelHandlerReceipt> results = new HashSet<>();
        final Map<String, Channel> channels = channelConfiguration.getChannels();
        for (Channel channel : channels.values()) {
            final Map<String, Object> environment = new HashMap<>();
            final List<String> messages = channel.getTriggers()
                    .values()
                    .stream()
                    .filter(triggerClass::isInstance)
                    .map(x -> {
                        final TriggerHandleReceipt receipt = x.handle(object);
                        if (receipt instanceof TriggerHandleReceipt.Handled) {
                            final TriggerHandleReceipt.Handled handled = (TriggerHandleReceipt.Handled) receipt;
                            environment.putAll(handled.getEnvironment());
                            return handled.getMessages();
                        }
                        if (receipt instanceof TriggerHandleReceipt.Unhandled) {
                            final List<String> strings = (List) Collections.emptyList();
                            return strings;
                        }
                        throw new NoSuchElementException();
                    })
                    .flatMap(Collection::stream)
                    .collect(Collectors.toList());

            if (messages.isEmpty()) {
                continue;
            }

            // 添加频道变量
            environment.put("channel", channel);
            results.add(new ChannelHandlerReceipt(channel, messages, environment));
        }

        return results;
    }

    public void channelHandle(Object object) {
        triggerHandle(object, QQTrigger.class)
                .forEach(x -> {
            final List<String> messages = x.messages.stream()
                    .map(y -> plugin.getXiaomingBot().getLanguageManager().formatAdditional(y, x.environment::get))
                    .collect(Collectors.toList());

            x.channel.getScopes()
                    .stream()
                    .filter(ServerScope.class::isInstance)
                    .forEach(y -> y.sendMessage(messages));
        });
    }

    public void channelHandle(Object object, OnlineClient onlineClient) {
        final Set<ChannelHandlerReceipt> receipts = triggerHandle(object, ServerTrigger.class);

        final Map<String, Object> environment = new HashMap<>();
        final ServerInfo serverInfo = onlineClient.getServerInfo();
        environment.put("server", serverInfo);

        receipts.forEach(x -> {
            final List<String> messages = x.messages.stream()
                    .map(y -> {
                        final String firstFormat = plugin.getXiaomingBot().getLanguageManager().formatAdditional(y, x.environment::get);
                        final String secondFormat = plugin.getXiaomingBot().getLanguageManager().formatAdditional(firstFormat, environment::get);
                        return secondFormat;
                    })
                    .collect(Collectors.toList());

            x.channel.getScopes()
                    .stream()
                    .filter(QQScope.class::isInstance)
                    .forEach(y -> y.sendMessage(messages));
        });
    }
}
