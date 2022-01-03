package cn.chuanwise.xiaoming.minecraft.xiaoming.interactors;

import cn.chuanwise.toolkit.container.Container;
import cn.chuanwise.util.CollectionUtil;
import cn.chuanwise.util.ConditionUtil;
import cn.chuanwise.util.StringUtil;
import cn.chuanwise.xiaoming.annotation.Filter;
import cn.chuanwise.xiaoming.annotation.FilterParameter;
import cn.chuanwise.xiaoming.annotation.Required;
import cn.chuanwise.xiaoming.interactor.SimpleInteractors;
import cn.chuanwise.xiaoming.minecraft.xiaoming.Plugin;
import cn.chuanwise.xiaoming.minecraft.xiaoming.channel.Channel;
import cn.chuanwise.xiaoming.minecraft.xiaoming.channel.scope.GroupScope;
import cn.chuanwise.xiaoming.minecraft.xiaoming.channel.scope.PrivateScope;
import cn.chuanwise.xiaoming.minecraft.xiaoming.channel.scope.Scope;
import cn.chuanwise.xiaoming.minecraft.xiaoming.channel.scope.ServerScope;
import cn.chuanwise.xiaoming.minecraft.xiaoming.channel.trigger.*;
import cn.chuanwise.xiaoming.minecraft.xiaoming.channel.trigger.server.*;
import cn.chuanwise.xiaoming.minecraft.xiaoming.channel.trigger.xiaoming.*;
import cn.chuanwise.xiaoming.minecraft.xiaoming.configuration.ChannelConfiguration;
import cn.chuanwise.xiaoming.minecraft.xiaoming.util.Words;
import cn.chuanwise.xiaoming.user.XiaomingUser;
import cn.chuanwise.xiaoming.util.InteractorUtil;
import cn.chuanwise.xiaoming.util.MiraiCodeUtil;

import java.util.*;
import java.util.regex.PatternSyntaxException;
import java.util.stream.Collectors;

@SuppressWarnings("all")
public class ChannelInteractors extends SimpleInteractors<Plugin> {
    @Override
    public void onRegister() {
        xiaomingBot.getInteractorManager().registerParameterParser(Channel.class, context -> {
            final XiaomingUser user = context.getUser();
            final String inputValue = context.getInputValue();

            final ChannelConfiguration channelConfiguration = plugin.getChannelConfiguration();
            final Channel channel = channelConfiguration.getChannels().get(inputValue);

            if (Objects.isNull(channel)) {
                user.sendError("找不到频道「" + inputValue + "」");
                return null;
            } else {
                return Container.of(channel);
            }
        }, true, plugin);

        xiaomingBot.getInteractorManager().registerParameterParser(Trigger.class, context -> {
            final XiaomingUser user = context.getUser();
            final String inputValue = context.getInputValue();

            final Object channelObject = context.getArgumentValues().get("频道");
            ConditionUtil.checkState(channelObject instanceof Channel, "internal error");
            final Channel channel = (Channel) channelObject;

            final Optional<Trigger<?>> optionalTrigger = channel.getTrigger(inputValue);
            if (!optionalTrigger.isPresent()) {
                user.sendError("频道「" + channel.getName() + "」下没有触发器「" + inputValue + "」");
                return null;
            } else {
                return (Container) Container.ofOptional(optionalTrigger);
            }
        }, true, plugin);
    }

    @Filter(Words.CHANNEL)
    @Required("xmmc.admin.channel.list")
    void channel(XiaomingUser user) {
        final ChannelConfiguration channelConfiguration = plugin.getChannelConfiguration();
        final Map<String, Channel> channels = channelConfiguration.getChannels();

        if (channels.isEmpty()) {
            user.sendWarning("没有开设任何频道哦");
        } else {
            user.sendMessage("现有的频道有：\n" + CollectionUtil.toIndexString(channels.keySet()));
        }
    }

    @Filter(Words.ADD + Words.CHANNEL + " {频道}")
    @Filter(Words.NEW + Words.CHANNEL + " {频道}")
    @Required("xmmc.admin.channel.add")
    void addChannel(XiaomingUser user, @FilterParameter("频道") String name) {
        final ChannelConfiguration channelConfiguration = plugin.getChannelConfiguration();
        final Map<String, Channel> channels = channelConfiguration.getChannels();

        if (channels.containsKey(name)) {
            user.sendError("已经存在名为「" + name + "」的频道了");
            return;
        }

        final Channel channel = new Channel();
        channel.setName(name);
        channels.put(name, channel);
        channelConfiguration.readyToSave();

        user.sendMessage("成功创建频道");
    }

    @Filter(Words.REMOVE + Words.CHANNEL + " {频道}")
    @Required("xmmc.admin.channel.remove")
    void removeChannel(XiaomingUser user, @FilterParameter("频道") Channel channel) {
        final ChannelConfiguration channelConfiguration = plugin.getChannelConfiguration();
        channelConfiguration.getChannels().remove(channel.getName());
        channelConfiguration.readyToSave();

        user.sendMessage("成功删除频道「" + channel.getName() + "」");
    }

    void addChannelScope0(XiaomingUser user, Channel channel, Scope scope) {
        final Set<Scope> scopes = channel.getScopes();
        if (scopes.add(scope)) {
            user.sendMessage("成功为频道「" + channel.getName() + "」增加了影响范围「" + scope.getDescription() + "」");
            plugin.getChannelConfiguration().readyToSave();
        } else {
            user.sendMessage("频道「" + channel.getName() + "」已经包含该范围了");
        }
    }

    @Filter(Words.ADD + Words.CHANNEL + Words.GROUP + Words.SCOPE + " {频道} {群标签}")
    @Required("xmmc.admin.channel.scope.add")
    void addChannelGroupScope(XiaomingUser user, @FilterParameter("频道") Channel channel, @FilterParameter("群标签") String groupTag) {
        addChannelScope0(user, channel, new GroupScope(groupTag));
    }

    @Filter(Words.ADD + Words.CHANNEL + Words.PRIVATE + Words.SCOPE + " {频道} {用户标签}")
    @Required("xmmc.admin.channel.scope.add")
    void addChannelPrivateScope(XiaomingUser user, @FilterParameter("频道") Channel channel, @FilterParameter("用户标签") String groupTag) {
        addChannelScope0(user, channel, new PrivateScope(groupTag));
    }

    @Filter(Words.ADD + Words.CHANNEL + Words.SERVER + Words.SCOPE + " {频道} {服务器标签}")
    @Required("xmmc.admin.channel.scope.add")
    void addChannelServerScope(XiaomingUser user, @FilterParameter("频道") Channel channel, @FilterParameter("服务器标签") String serverTag) {
        addChannelScope0(user, channel, new ServerScope(serverTag));
    }

    void removeChannelScope0(XiaomingUser user, Channel channel, Scope scope) {
        final Set<Scope> scopes = channel.getScopes();
        if (scopes.remove(scope)) {
            user.sendMessage("成功删除频道「" + channel.getName() + "」的影响范围「" + scope.getDescription() + "」");
            plugin.getChannelConfiguration().readyToSave();
        } else {
            user.sendError("频道「" + channel.getName() + "」并没有包含范围「" + scope.getDescription() + "」");
        }
    }

    @Filter(Words.REMOVE + Words.CHANNEL + Words.GROUP + Words.SCOPE + " {频道} {群标签}")
    @Required("xmmc.admin.channel.scope.remove")
    void removeChannelGroupScope(XiaomingUser user, @FilterParameter("频道") Channel channel, @FilterParameter("群标签") String groupTag) {
        removeChannelScope0(user, channel, new GroupScope(groupTag));
    }

    @Filter(Words.REMOVE + Words.CHANNEL + Words.PRIVATE + Words.SCOPE + " {频道} {用户标签}")
    @Required("xmmc.admin.channel.scope.remove")
    void removeChannelPrivateScope(XiaomingUser user, @FilterParameter("频道") Channel channel, @FilterParameter("用户标签") String accountTag) {
        removeChannelScope0(user, channel, new PrivateScope(accountTag));
    }

    @Filter(Words.REMOVE + Words.CHANNEL + Words.SERVER + Words.SCOPE + " {频道} {服务器标签}")
    @Required("xmmc.admin.channel.scope.remove")
    void removeChannelServerScope(XiaomingUser user, @FilterParameter("频道") Channel channel, @FilterParameter("服务器标签") String serverTag) {
        removeChannelScope0(user, channel, new ServerScope(serverTag));
    }

    @Filter(Words.CHANNEL + " {频道}")
    @Required("xmmc.admin.channel.look")
    void channelInfo(XiaomingUser user, @FilterParameter("频道") Channel channel) {
        user.sendMessage("「频道信息」\n" +
                "频道名：" + channel.getName() + "\n" +
                "范围：" + Optional.ofNullable(CollectionUtil.toIndexString(channel.getScopes(), Scope::getDescription))
                        .map(x -> "\n" + x)
                        .orElse("（无）") + "\n" +
                "触发器：" + Optional.ofNullable(CollectionUtil.toIndexString(channel.getTriggers().keySet()))
                        .map(x -> "\n" + x)
                        .orElse("（无）")
        );
    }

    @Filter(Words.CHANNEL + Words.TRIGGER + " {频道}")
    @Required("xmmc.admin.channel.look")
    void channelTriggerInfo(XiaomingUser user, @FilterParameter("频道") Channel channel) {
        final Map<String, Trigger<?>> triggers = channel.getTriggers();
        if (triggers.isEmpty()) {
            user.sendError("频道「" + channel.getName() + "」没有任何触发器");
            return;
        }

        user.sendMessage("「频道触发器信息」\n" +
                CollectionUtil.toIndexString(triggers.entrySet(), x -> x.getKey() + "：" + x.getValue().getDescription())
        );
    }

    @Filter(Words.CHANNEL + Words.BROADCAST + " {频道} {r:消息}")
    @Filter(Words.SEND + Words.CHANNEL + Words.MESSAGE + " {频道} {r:消息}")
    @Required("xmmc.admin.channel.broadcast")
    void channelBroadcast(XiaomingUser user, @FilterParameter("频道") Channel channel, @FilterParameter("消息") String message) {
        final Set<Scope> scopes = channel.getScopes();
        if (scopes.isEmpty()) {
            user.sendMessage("该频道没有包含任何范围，消息不会被发送");
        } else {
            scopes.forEach(x -> x.sendMessage(Arrays.asList(message)));
            user.sendMessage("成功将消息发送到" + scopes.size() + " 个范围");
        }
    }

    void configTriggerNameThenAdd(XiaomingUser user, Channel channel, Trigger<?> trigger) {
        user.sendMessage("为这个触发器起一个名字吧！");
        while (true) {
            final String name = user.nextMessageOrExit().serialize();
            final Optional<Trigger<?>> optionalTrigger = channel.getTrigger(name);
            if (optionalTrigger.isPresent()) {
                user.sendError("已经存在同名触发器，换个名字再试一次吧");
                continue;
            }

            trigger.setName(name);
            channel.getTriggers().put(name, trigger);
            break;
        }

        plugin.getChannelConfiguration().readyToSave();
        user.sendMessage("成功为频道「" + channel.getName() + "」添加触发器「" + trigger.getDescription() + "」");
    }

    void configTriggerMessages(XiaomingUser user, Channel channel, Trigger<?> trigger) {
        final ChannelConfiguration.DefaultValue defaultValue = plugin.getChannelConfiguration().getDefaultValue();
        String defaultMessage = null;
        if (trigger instanceof GroupMessageTrigger) {
            defaultMessage = defaultValue.getGroupMessageTriggerMessage();
        } else if (trigger instanceof PlayerChatTrigger) {
            defaultMessage = defaultValue.getPlayerChatTriggerMessage();
        }

        // 没有默认值
        if (StringUtil.isEmpty(defaultMessage)) {
            user.sendMessage("触发器被激活时，将会产生哪些消息？逐条告诉小明吧，使用「结束」结束");

            final List<String> message = new ArrayList<>();
            final List<String> translatedMessage = InteractorUtil.fillStringCollection(user, message, "触发器消息")
                    .stream()
                    .map(MiraiCodeUtil::contentToString)
                    .collect(Collectors.toList());

            trigger.setMessages(translatedMessage);
        } else {
            user.sendMessage("触发器被激活时，将会产生哪些消息？\n" +
                    "小明建议你将其设置为「" + defaultMessage + "」，接受建议请回复「接受」。\n" +
                    "或者逐条告诉小明你希望触发器激活时产生的消息，使用「结束」结束");

            final String firstMessage = user.nextMessageOrExit().serialize();
            final List<String> messages = new ArrayList<>();
            if (Objects.equals(firstMessage, "接受")) {
                messages.add(defaultMessage);
                trigger.setMessages(messages);
            } else {
                messages.add(firstMessage);
                final List<String> translatedMessage = InteractorUtil.fillStringCollection(user, messages, "触发器消息")
                        .stream()
                        .map(MiraiCodeUtil::contentToString)
                        .collect(Collectors.toList());
                trigger.setMessages(translatedMessage);
            }
        }
    }

    void configTriggerGroupTag(XiaomingUser user, Channel channel, GroupMessageTrigger trigger) {
        user.sendMessage("哪些群的消息能激活该触发器？告诉小明它们的标签吧");
        trigger.setGroupTag(user.nextMessageOrExit().serialize());
    }

    void configTriggerAccountTag(XiaomingUser user, Channel channel, AccountTagTrigger trigger) {
        user.sendMessage("哪些 QQ 用户能激活该触发器呢？告诉小明他们的标签吧");
        trigger.setAccountTag(user.nextMessageOrExit().serialize());
    }

    void configTriggerAccountTag(XiaomingUser user, Channel channel, PlayerChatTrigger trigger) {
        user.sendMessage("服务器玩家需要绑定 QQ 才能激活该触发器吗？回复「是」，或其他任意内容");
        final String reply = user.nextMessageOrExit().serialize();
        if (Objects.equals(reply, "是")) {
            user.sendMessage("这些绑定的 QQ 用户需要具备什么标签才能激活该触发器呢？");
            trigger.setAccountTag(user.nextMessageOrExit().serialize());
        } else {
            trigger.setAccountTag(null);
        }
    }

    void configTriggerServerTag(XiaomingUser user, Channel channel, ServerTagTrigger trigger) {
        user.sendMessage("哪些服务器上的人能激活该触发器呢？告诉小明它们的标签吧");
        trigger.setServerTag(user.nextMessageOrExit().serialize());
    }

    void configTriggerPermission(XiaomingUser user, Channel channel, QQTrigger<?> trigger) {
        user.sendMessage("每个 QQ 需要具备权限才能触发吗？回复我「不需要」或权限节点。");
        final String reply = user.nextMessageOrExit().serialize();
        if (Objects.equals(reply, "不需要")) {
            trigger.setPermission("");
        } else {
            trigger.setPermission(reply);
        }
    }

    void configTriggerMessageFilter(XiaomingUser user, Channel channel, MessageFilterTrigger trigger) {
        user.sendMessage("哪些消息能激活这个触发器呢？\n" +
                "回复我下面的任何一条规则（如「包含」），其他任意内容将使小明允许所有消息触发。\n" +
                "开头包含、结尾包含、包含、匹配、开头匹配、结尾匹配、包含匹配、格式匹配");
        final String reply = user.nextMessageOrExit().serialize();
        switch (reply) {
            case "开头包含":
                user.sendMessage("以什么开头呢？");
                trigger.setMessageFilter(new MessageFilter.StartWith(user.nextMessageOrExit().serialize()));
                break;
            case "结尾包含":
                user.sendMessage("以什么结尾呢？");
                trigger.setMessageFilter(new MessageFilter.EndWith(user.nextMessageOrExit().serialize()));
                break;
            case "包含":
                user.sendMessage("包含什么内容呢？");
                trigger.setMessageFilter(new MessageFilter.ContainsEqual(user.nextMessageOrExit().serialize()));
                break;
            case "匹配":
                user.sendMessage("匹配哪一个正则表达式呢？");
                while (true) {
                    final String input = user.nextMessageOrExit().serialize();
                    final String content = MiraiCodeUtil.contentToString(input);

                    try {
                        trigger.setMessageFilter(new MessageFilter.Match(content));
                        break;
                    } catch (PatternSyntaxException exception) {
                        user.sendError("编译错误：" + exception.getDescription() + "，请重新输入正则表达式");
                    }
                }
                break;
            case "开头匹配":
                user.sendMessage("开头匹配哪一个正则表达式呢？");
                while (true) {
                    final String input = user.nextMessageOrExit().serialize();
                    final String content = MiraiCodeUtil.contentToString(input);

                    try {
                        trigger.setMessageFilter(new MessageFilter.StartMatch(content));
                        break;
                    } catch (PatternSyntaxException exception) {
                        user.sendError("编译错误：" + exception.getDescription() + "，请重新输入正则表达式");
                    }
                }
                break;
            case "结尾匹配":
                user.sendMessage("结尾匹配哪一个正则表达式呢？");
                while (true) {
                    final String input = user.nextMessageOrExit().serialize();
                    final String content = MiraiCodeUtil.contentToString(input);

                    try {
                        trigger.setMessageFilter(new MessageFilter.EndMatch(content));
                        break;
                    } catch (PatternSyntaxException exception) {
                        user.sendError("编译错误：" + exception.getDescription() + "，请重新输入正则表达式");
                    }
                }
                break;
            case "包含匹配":
                user.sendMessage("包含匹配哪一个正则表达式呢？");
                while (true) {
                    final String input = user.nextMessageOrExit().serialize();
                    final String content = MiraiCodeUtil.contentToString(input);

                    try {
                        trigger.setMessageFilter(new MessageFilter.ContainMatch(content));
                        break;
                    } catch (PatternSyntaxException exception) {
                        user.sendError("编译错误：" + exception.getDescription() + "，请重新输入正则表达式");
                    }
                }
                break;
            case "格式匹配":
                user.sendMessage("消息应该是什么格式呢？");
                while (true) {
                    final String input = user.nextMessageOrExit().serialize();
                    final String content = MiraiCodeUtil.contentToString(input);

                    try {
                        trigger.setMessageFilter(new MessageFilter.Format(content));
                        break;
                    } catch (IllegalArgumentException exception) {
                        user.sendError("编译错误：" + exception.getMessage() + "，请重新输入格式");
                    }
                }
                break;
            default:
                trigger.setMessageFilter(new MessageFilter.All());
                user.sendMessage("任何消息皆可触发");
                break;
        }
    }

    void configTriggerBind(XiaomingUser user, Channel channel, QQTrigger<?> trigger) {
        user.sendMessage("每个 QQ 绑定玩家名后才能触发吗？回复我「是」，或其他任意内容");
        final String reply = user.nextMessageOrExit().serialize();
        trigger.setMustBind(Objects.equals(reply, "是"));
    }

    @Filter(Words.ADD + Words.PLAYER + Words.DEATH + Words.TRIGGER + " {频道}")
    @Filter(Words.ADD + Words.CHANNEL + Words.PLAYER + Words.DEATH + Words.TRIGGER + " {频道}")
    @Required("xmmc.admin.channel.trigger.add")
    void addChannelPlayerDeathTrigger(XiaomingUser user, @FilterParameter("频道") Channel channel) {
        final PlayerDeathTrigger trigger = new PlayerDeathTrigger();
        configTriggerServerTag(user, channel, trigger);
        configTriggerAccountTag(user, channel, trigger);
        configTriggerMessages(user, channel, trigger);
        configTriggerNameThenAdd(user, channel, trigger);
    }

    @Filter(Words.ADD + Words.PLAYER + Words.JOIN + Words.TRIGGER + " {频道}")
    @Filter(Words.ADD + Words.CHANNEL + Words.PLAYER + Words.JOIN + Words.TRIGGER + " {频道}")
    @Required("xmmc.admin.channel.trigger.add")
    void addChannelPlayerJoinTrigger(XiaomingUser user, @FilterParameter("频道") Channel channel) {
        final PlayerJoinTrigger trigger = new PlayerJoinTrigger();
        configTriggerServerTag(user, channel, trigger);
        configTriggerAccountTag(user, channel, trigger);
        configTriggerMessages(user, channel, trigger);
        configTriggerNameThenAdd(user, channel, trigger);
    }

    @Filter(Words.ADD + Words.PLAYER + Words.QUIT + Words.TRIGGER + " {频道}")
    @Filter(Words.ADD + Words.CHANNEL + Words.PLAYER + Words.QUIT + Words.TRIGGER + " {频道}")
    @Required("xmmc.admin.channel.trigger.add")
    void addChannelPlayerQuitTrigger(XiaomingUser user, @FilterParameter("频道") Channel channel) {
        final PlayerQuitTrigger trigger = new PlayerQuitTrigger();
        configTriggerServerTag(user, channel, trigger);
        configTriggerAccountTag(user, channel, trigger);
        configTriggerMessages(user, channel, trigger);
        configTriggerNameThenAdd(user, channel, trigger);
    }

    @Filter(Words.ADD + Words.PLAYER + Words.CHANGE_WORLD + Words.TRIGGER + " {频道}")
    @Filter(Words.ADD + Words.CHANNEL + Words.PLAYER + Words.CHANGE_WORLD + Words.TRIGGER + " {频道}")
    @Required("xmmc.admin.channel.trigger.add")
    void addChannelPlayerChangeWorldTrigger(XiaomingUser user, @FilterParameter("频道") Channel channel) {
        final PlayerChangeWorldTrigger trigger = new PlayerChangeWorldTrigger();
        configTriggerServerTag(user, channel, trigger);
        configTriggerAccountTag(user, channel, trigger);
        configTriggerMessages(user, channel, trigger);
        configTriggerNameThenAdd(user, channel, trigger);
    }

    @Filter(Words.ADD + Words.PLAYER + Words.CHAT + Words.TRIGGER + " {频道}")
    @Filter(Words.ADD + Words.CHANNEL + Words.PLAYER + Words.CHAT + Words.TRIGGER + " {频道}")
    @Required("xmmc.admin.channel.trigger.add")
    void addChannelPlayerChatTrigger(XiaomingUser user, @FilterParameter("频道") Channel channel) {
        final PlayerChatTrigger trigger = new PlayerChatTrigger();
        configTriggerServerTag(user, channel, trigger);
        configTriggerAccountTag(user, channel, trigger);
        configTriggerMessageFilter(user, channel, trigger);
        configTriggerMessages(user, channel, trigger);
        configTriggerNameThenAdd(user, channel, trigger);
    }

    @Filter(Words.ADD + Words.GROUP + Words.MESSAGE + Words.TRIGGER + " {频道}")
    @Filter(Words.ADD + Words.CHANNEL + Words.GROUP + Words.MESSAGE + Words.TRIGGER + " {频道}")
    @Required("xmmc.admin.channel.trigger.add")
    void addChannelGroupChatTrigger(XiaomingUser user, @FilterParameter("频道") Channel channel) {
        final GroupMessageTrigger trigger = new GroupMessageTrigger();
        configTriggerAccountTag(user, channel, trigger);
        configTriggerGroupTag(user, channel, trigger);
        configTriggerBind(user, channel, trigger);
        configTriggerMessageFilter(user, channel, trigger);
        configTriggerPermission(user, channel, trigger);
        configTriggerMessages(user, channel, trigger);
        configTriggerNameThenAdd(user, channel, trigger);
    }

    @Filter(Words.ADD + Words.PRIVATE + Words.MESSAGE + Words.TRIGGER + " {频道}")
    @Filter(Words.ADD + Words.CHANNEL + Words.PRIVATE + Words.MESSAGE + Words.TRIGGER + " {频道}")
    @Required("xmmc.admin.channel.trigger.add")
    void addChannelPrivateChatTrigger(XiaomingUser user, @FilterParameter("频道") Channel channel) {
        final PrivateMessageTrigger trigger = new PrivateMessageTrigger();
        configTriggerAccountTag(user, channel, trigger);
        configTriggerBind(user, channel, trigger);
        configTriggerMessageFilter(user, channel, trigger);
        configTriggerPermission(user, channel, trigger);
        configTriggerMessages(user, channel, trigger);
        configTriggerNameThenAdd(user, channel, trigger);
    }

    @Filter(Words.ADD + Words.MEMBER + Words.MUTE + Words.TRIGGER + " {频道}")
    @Filter(Words.ADD + Words.CHANNEL + Words.MEMBER + Words.MUTE + Words.TRIGGER + " {频道}")
    @Required("xmmc.admin.channel.trigger.add")
    void addChannelMemberMuteTrigger(XiaomingUser user, @FilterParameter("频道") Channel channel) {
        final MemberMuteTrigger trigger = new MemberMuteTrigger();
        configTriggerAccountTag(user, channel, trigger);
        configTriggerBind(user, channel, trigger);
        configTriggerPermission(user, channel, trigger);
        configTriggerMessages(user, channel, trigger);
        configTriggerNameThenAdd(user, channel, trigger);
    }

    @Filter(Words.REMOVE + Words.CHANNEL + Words.TRIGGER + " {频道} {触发器}")
    @Required("xmmc.admin.channel.trigger.remove")
    void removeChannelTrigger(XiaomingUser user, @FilterParameter("频道") Channel channel, @FilterParameter("触发器") Trigger trigger) {
        channel.getTriggers().remove(trigger.getName());
        plugin.getChannelConfiguration().readyToSave();
        user.sendMessage("已删除频道「" + channel.getName() + "」的触发器「" + trigger.getName() + "」");
    }

    @Filter(Words.CHANNEL + Words.TRIGGER + " {频道} {触发器}")
    @Required("xmmc.admin.channel.trigger.look")
    void channelTriggerInfo(XiaomingUser user, @FilterParameter("频道") Channel channel, @FilterParameter("触发器") Trigger trigger) {
        user.sendMessage("「触发器信息」\n" +
                "描述：" + trigger.getDescription() + "\n" +
                "消息：" + Optional.ofNullable(CollectionUtil.toIndexString(trigger.getMessages()))
                            .map(x -> "\n" + x)
                            .orElse("（无）"));
    }
}