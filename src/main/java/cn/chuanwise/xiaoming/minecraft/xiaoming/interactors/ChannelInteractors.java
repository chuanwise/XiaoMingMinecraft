package cn.chuanwise.xiaoming.minecraft.xiaoming.interactors;

import cn.chuanwise.toolkit.container.Container;
import cn.chuanwise.util.*;
import cn.chuanwise.util.Collections;
import cn.chuanwise.xiaoming.annotation.Filter;
import cn.chuanwise.xiaoming.annotation.FilterParameter;
import cn.chuanwise.xiaoming.annotation.Required;
import cn.chuanwise.xiaoming.interactor.SimpleInteractors;
import cn.chuanwise.xiaoming.minecraft.xiaoming.XMMCXiaoMingPlugin;
import cn.chuanwise.xiaoming.minecraft.xiaoming.channel.Channel;
import cn.chuanwise.xiaoming.minecraft.xiaoming.channel.WorkGroup;
import cn.chuanwise.xiaoming.minecraft.xiaoming.channel.executor.Executor;
import cn.chuanwise.xiaoming.minecraft.xiaoming.channel.executor.ServerTagExecutor;
import cn.chuanwise.xiaoming.minecraft.xiaoming.channel.executor.server.ServerBroadcastExecutor;
import cn.chuanwise.xiaoming.minecraft.xiaoming.channel.executor.server.ServerConsoleCommandExecutor;
import cn.chuanwise.xiaoming.minecraft.xiaoming.channel.executor.xiaoming.XiaoMingSendMessageExecutor;
import cn.chuanwise.xiaoming.minecraft.xiaoming.channel.trigger.*;
import cn.chuanwise.xiaoming.minecraft.xiaoming.channel.trigger.server.*;
import cn.chuanwise.xiaoming.minecraft.xiaoming.channel.trigger.xiaoming.*;
import cn.chuanwise.xiaoming.minecraft.xiaoming.configuration.ChannelConfiguration;
import cn.chuanwise.xiaoming.minecraft.xiaoming.util.Words;
import cn.chuanwise.xiaoming.user.XiaoMingUser;
import cn.chuanwise.xiaoming.util.Interactors;
import cn.chuanwise.xiaoming.util.MiraiCodes;

import java.util.*;
import java.util.function.Function;
import java.util.regex.PatternSyntaxException;

@SuppressWarnings("all")
public class ChannelInteractors extends SimpleInteractors<XMMCXiaoMingPlugin> {
    @Override
    public void onRegister() {
        xiaoMingBot.getInteractorManager().registerParameterParser(Channel.class, context -> {
            final XiaoMingUser user = context.getUser();
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

        xiaoMingBot.getInteractorManager().registerParameterParser(WorkGroup.class, context -> {
            final XiaoMingUser user = context.getUser();
            final String inputValue = context.getInputValue();

            final Object channelObject = context.getArgumentValues().get("频道");
            Preconditions.state(channelObject instanceof Channel, "internal error");
            final Channel channel = (Channel) channelObject;

            final Optional<WorkGroup> optionalWorkGroup = channel.getWorkGroup(inputValue);
            if (!optionalWorkGroup.isPresent()) {
                user.sendError("频道「" + channel.getName() + "」内没有工作组「" + inputValue + "」");
                return null;
            } else {
                return (Container) Container.ofOptional(optionalWorkGroup);
            }
        }, true, plugin);
    }

    @Filter(Words.CHANNEL)
    @Required("xmmc.admin.channel.list")
    void channel(XiaoMingUser user) {
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
    void addChannel(XiaoMingUser user, @FilterParameter("频道") String name) {
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

        user.sendMessage("成功创建空频道「" + name + "」，你可以用「添加工作组 " + name + " [工作组名]」为频道部署工作组");
    }

    @Filter(Words.REMOVE + Words.CHANNEL + " {频道}")
    @Required("xmmc.admin.channel.remove")
    void removeChannel(XiaoMingUser user, @FilterParameter("频道") Channel channel) {
        final ChannelConfiguration channelConfiguration = plugin.getChannelConfiguration();
        channelConfiguration.getChannels().remove(channel.getName());
        channelConfiguration.readyToSave();

        user.sendMessage("成功删除频道「" + channel.getName() + "」");
    }

    @Filter(Words.CHANNEL + " {频道}")
    @Required("xmmc.admin.channel.look")
    void channelInfo(XiaoMingUser user, @FilterParameter("频道") Channel channel) {
        user.sendMessage("「频道信息」\n" +
                "频道名：" + channel.getName() + "\n" +
                "状态：" + (channel.isEnable() ? "已启动" : "已关闭") + "\n" +
                "工作组：" + Optional.ofNullable(CollectionUtil.toIndexString(channel.getWorkGroups().keySet()))
                        .map(x -> "\n" + x)
                        .orElse("（无）")
        );
    }

    @Filter(Words.ENABLE + Words.CHANNEL + " {频道}")
    @Required("xmmc.admin.channel.enable")
    void enableChannel(XiaoMingUser user, @FilterParameter("频道") Channel channel) {
        if (channel.isEnable()) {
            user.sendError("频道「" + channel.getName() + "」已经启动了");
        } else {
            channel.setEnable(true);
            plugin.getChannelConfiguration().readyToSave();
            user.sendError("成功启动频道「" + channel.getName() + "」");
        }
    }

    @Filter(Words.DISABLE + Words.CHANNEL + " {频道}")
    @Required("xmmc.admin.channel.disable")
    void disableChannel(XiaoMingUser user, @FilterParameter("频道") Channel channel) {
        if (channel.isEnable()) {
            channel.setEnable(false);
            plugin.getChannelConfiguration().readyToSave();
            user.sendError("成功关闭频道「" + channel.getName() + "」");
        } else {
            user.sendError("频道「" + channel.getName() + "」尚未启动");
        }
    }

    private final Map<Class<?>, Function<XiaoMingUser, Trigger<?>>> triggerConfigurers = new HashMap<>();
    {
        triggerConfigurers.put(PlayerChangeWorldTrigger.class, user -> {
            final PlayerChangeWorldTrigger trigger = new PlayerChangeWorldTrigger();
            configServerTriggerServerTag(user, trigger);
            configServerTriggerMustBindCompletely(user, trigger);
            return trigger;
        });
        triggerConfigurers.put(PlayerChatTrigger.class, user -> {
            final PlayerChatTrigger trigger = new PlayerChatTrigger();
            configServerTriggerServerTag(user, trigger);
            configTriggerMessageFilter(user, trigger);
            configServerTriggerMustBindCompletely(user, trigger);
            return trigger;
        });
        triggerConfigurers.put(PlayerDeathTrigger.class, user -> {
            final PlayerDeathTrigger trigger = new PlayerDeathTrigger();
            configServerTriggerServerTag(user, trigger);
            configServerTriggerMustBindCompletely(user, trigger);
            return trigger;
        });
        triggerConfigurers.put(PlayerJoinTrigger.class, user -> {
            final PlayerJoinTrigger trigger = new PlayerJoinTrigger();
            configServerTriggerServerTag(user, trigger);
            configServerTriggerMustBindCompletely(user, trigger);
            return trigger;
        });
        triggerConfigurers.put(PlayerQuitTrigger.class, user -> {
            final PlayerQuitTrigger trigger = new PlayerQuitTrigger();
            configServerTriggerServerTag(user, trigger);
            configServerTriggerMustBindCompletely(user, trigger);
            return trigger;
        });

        triggerConfigurers.put(GroupMessageTrigger.class, user -> {
            final GroupMessageTrigger trigger = new GroupMessageTrigger();
            configXiaoMingTriggerGroupTag(user, trigger);
            configTriggerMessageFilter(user, trigger);
            configXiaoMingTriggerMustBindCompletely(user, trigger);
            return trigger;
        });
        triggerConfigurers.put(PrivateMessageTrigger.class, user -> {
            final PrivateMessageTrigger trigger = new PrivateMessageTrigger();
            configTriggerMessageFilter(user, trigger);
            configXiaoMingTriggerMustBindCompletely(user, trigger);
            return trigger;
        });
        triggerConfigurers.put(MemberMuteTrigger.class, user -> {
            final MemberMuteTrigger trigger = new MemberMuteTrigger();
            configXiaoMingTriggerMustBindCompletely(user, trigger);
            return trigger;
        });
    }

    void configXiaoMingTriggerGroupTag(XiaoMingUser user, GroupTagTrigger trigger) {
        user.sendMessage("服务器玩家绑定的 QQ 至少要带有什么标签才能激活该触发器呢？回复「所有」或一个标签");
        final String reply = user.nextMessageOrExit().serialize();

        final String groupTag;
        if (Objects.equals(reply, "所有")) {
            groupTag = null;
        } else {
            groupTag = reply;
        }
        trigger.setGroupTag(groupTag);
    }

    /** Account Tag */
    void configServerTriggerAccountTag(XiaoMingUser user, AccountTagTrigger trigger) {
        user.sendMessage("服务器玩家绑定的 QQ 至少要带有什么标签才能激活该触发器呢？回复「无」或一个标签");
        final String reply = user.nextMessageOrExit().serialize();

        final String accountTag;
        if (Objects.equals(reply, "无")) {
            accountTag = null;
        } else {
            accountTag = reply;
        }
        trigger.setAccountTag(accountTag);
    }

    void configXiaoMingTriggerAccountTag(XiaoMingUser user, AccountTagTrigger trigger) {
        user.sendMessage("用户要带有什么标签才能激活该触发器？回复「无」或一个标签");
        final String reply = user.nextMessageOrExit().serialize();

        final String accountTag;
        if (Objects.equals(reply, "无")) {
            accountTag = null;
        } else {
            accountTag = reply;
        }
        trigger.setAccountTag(accountTag);
    }

    /** Server Tag */
    void configServerTriggerServerTag(XiaoMingUser user, ServerTrigger trigger) {
        user.sendMessage("玩家要在带有什么标签的服务器上才能激活该触发器？回复「无」或一个标签");
        final String reply = user.nextMessageOrExit().serialize();

        final String serverTag;
        if (Objects.equals(reply, "无")) {
            serverTag = null;
        } else {
            serverTag = reply;
        }
        trigger.setServerTag(serverTag);
    }

    /** Must Bind */
    void configServerTriggerMustBind(XiaoMingUser user, BindableTrigger trigger) {
        user.sendMessage("玩家必须绑定 QQ 才能激活该触发器吗？回复「是」或其他任意内容");
        trigger.setMustBind(Objects.equals(user.nextMessageOrExit().serialize(), "是"));
    }

    void configXiaoMingTriggerMustBind(XiaoMingUser user, BindableTrigger trigger) {
        user.sendMessage("必须绑定玩家名才能激活该触发器吗？回复「是」或其他任意内容");
        trigger.setMustBind(Objects.equals(user.nextMessageOrExit().serialize(), "是"));
    }

    void configServerTriggerMustBindCompletely(XiaoMingUser user, BindableTrigger trigger) {
        configServerTriggerMustBind(user, trigger);
        if (trigger.isMustBind()) {
            configServerTriggerAccountTag(user, trigger);
            configServerTriggerXiaoMingPermission(user, trigger);
        }
    }

    void configXiaoMingTriggerMustBindCompletely(XiaoMingUser user, BindableTrigger trigger) {
        configXiaoMingTriggerMustBind(user, trigger);
        if (trigger.isMustBind()) {
            configXiaoMingTriggerPlayerPermission(user, trigger);
        }
    }

    /** Xiao Ming Permission */
    void configServerTriggerXiaoMingPermission(XiaoMingUser user, XiaoMingPermissionTrigger trigger) {
        user.sendMessage("玩家绑定的 QQ 至少要具备什么权限才能激活该触发器？回复「无」或小明权限节点");
        final String reply = user.nextMessageOrExit().serialize();

        final String permission;
        if (Objects.equals(reply, "无")) {
            permission = null;
        } else {
            permission = reply;
        }
        trigger.setXiaoMingPermission(permission);
    }

    void configXiaoMingTriggerXiaoMingPermission(XiaoMingUser user, XiaoMingPermissionTrigger trigger) {
        user.sendMessage("至少要具备什么权限才能激活该触发器？回复「无」或小明权限节点");
        final String reply = user.nextMessageOrExit().serialize();

        final String permission;
        if (Objects.equals(reply, "无")) {
            permission = null;
        } else {
            permission = reply;
        }
        trigger.setXiaoMingPermission(permission);
    }

    /** Player Permission */
    void configServerTriggerPlayerPermission(XiaoMingUser user, PlayerPermissionTrigger trigger) {
        user.sendMessage("玩家具备什么权限才能激活该触发器？回复「无」或 Minecraft 服务器权限节点");
        final String reply = user.nextMessageOrExit().serialize();

        final String permission;
        if (Objects.equals(reply, "无")) {
            permission = null;
        } else {
            permission = reply;
        }
        trigger.setPlayerPermission(permission);
    }

    void configXiaoMingTriggerPlayerPermission(XiaoMingUser user, PlayerPermissionTrigger trigger) {
        user.sendMessage("绑定的玩家名中，至少要有一个在线并具备什么权限才能激活该触发器？回复「无」或 Minecraft 服务器权限节点");
        final String reply = user.nextMessageOrExit().serialize();

        final String permission;
        if (Objects.equals(reply, "无")) {
            permission = null;
        } else {
            permission = reply;
        }
        trigger.setPlayerPermission(permission);
    }

    /** Message Filter */
    void configTriggerMessageFilter(XiaoMingUser user, MessageFilterTrigger trigger) {
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
                    final String content = MiraiCodes.contentToString(input);

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
                    final String content = MiraiCodes.contentToString(input);

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
                    final String content = MiraiCodes.contentToString(input);

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
                    final String content = MiraiCodes.contentToString(input);

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
                    final String content = MiraiCodes.contentToString(input);

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

    private final Map<Class<?>, Function<XiaoMingUser, Executor>> executorConfigurers = new HashMap<>();
    {
        executorConfigurers.put(ServerBroadcastExecutor.class, user -> {
            final ServerBroadcastExecutor executor = new ServerBroadcastExecutor();
            configExecutorServerTag(user, executor);

            user.sendMessage("要在服务器上广播什么消息？");
            executor.setFormat(MiraiCodes.contentToString(user.nextMessageOrExit().serialize()));

            return executor;
        });
        executorConfigurers.put(ServerConsoleCommandExecutor.class, user -> {
            final ServerConsoleCommandExecutor executor = new ServerConsoleCommandExecutor();
            configExecutorServerTag(user, executor);

            user.sendMessage("要在服务器上执行什么指令？");
            executor.setCommand(MiraiCodes.contentToString(user.nextMessageOrExit().serialize()));

            return executor;
        });

        executorConfigurers.put(XiaoMingSendMessageExecutor.class, user -> {
            final XiaoMingSendMessageExecutor executor = new XiaoMingSendMessageExecutor();
            user.sendMessage("要在 QQ 上发送什么消息？");
            executor.setFormat(user.nextMessageOrExit().serialize());
            return executor;
        });
    }

    /** Server Tag */
    void configExecutorServerTag(XiaoMingUser user, ServerTagExecutor trigger) {
        user.sendMessage("这个操作要在带有什么标签的服务器上进行？回复「所有」或服务器标签");
        final String reply = user.nextMessageOrExit().serialize();

        final String serverTag;
        if (Objects.equals(reply, "所有")) {
            serverTag = Tags.ALL;
        } else {
            serverTag = reply;
        }
        trigger.setServerTag(serverTag);
    }

    void config(XiaoMingUser user, XiaoMingSendMessageExecutor trigger) {
        user.sendMessage("要在 QQ 上发送什么消息？");
        trigger.setFormat(user.nextMessageOrExit().serialize());
    }

    @Filter(Words.ADD + Words.WORK_GROUP + " {频道} {工作组}")
    @Filter(Words.NEW + Words.WORK_GROUP + " {频道} {工作组}")
    @Filter(Words.ADD + Words.CHANNEL + Words.WORK_GROUP + " {频道} {工作组}")
    @Filter(Words.NEW + Words.CHANNEL + Words.WORK_GROUP + " {频道} {工作组}")
    @Required("xmmc.admin.channel.config")
    void addWorkGroup(XiaoMingUser user, @FilterParameter("频道") Channel channel, @FilterParameter("工作组") String workGroupName) {
        if (channel.getWorkGroup(workGroupName).isPresent()) {
            user.sendError("频道「" + channel.getName() + "」已经存在工作组「" + workGroupName + "」了");
            return;
        }

        // 找出向导
        final List<Class<?>> triggerClasses = Collections.asUnmodifiableList(Trigger.TRIGGER_NAMES.keySet());
        if (triggerClasses.isEmpty()) {
            user.sendError("插件内部错误，请反馈给作者！");
            return;
        }

        // 选择向导
        user.sendMessage("输入序号，选择工作组触发器类型吧！");
        Trigger<?> trigger;
        while (true) {
            final Class<?> triggerClass = Interactors.indexChooser(user, triggerClasses, Trigger.TRIGGER_NAMES::get, 20);
            final Function<XiaoMingUser, Trigger<?>> guider = triggerConfigurers.get(triggerClass);
            if (guider == null) {
                user.sendError("当前版本插件并未设计该类型触发器的向导，重新选择工作组触发器类型吧！");
            } else {
                trigger = guider.apply(user);
                break;
            }
        }

        // 配置执行器
        final List<Class<?>> executorClasses = Collections.asUnmodifiableList(Executor.EXECUTOR_NAMES.keySet());

        user.sendMessage("输入序号，为该工作组添加第一个执行器吧！");
        final Executor executor;
        while (true) {
            final Class<?> executorClass = Interactors.indexChooser(user, executorClasses, Executor.EXECUTOR_NAMES::get, 20);
            final Function<XiaoMingUser, Executor> guider = executorConfigurers.get(executorClass);
            if (guider == null) {
                user.sendError("当前版本插件并未设计该类型执行器的向导，重新选择工作组执行器类型吧！");
            } else {
                executor = guider.apply(user);
                break;
            }
        }

        final WorkGroup workGroup = new WorkGroup();
        workGroup.setName(workGroupName);
        workGroup.setTrigger(trigger);
        workGroup.setExecutors(Collections.asList(executor));
        channel.getWorkGroups().put(workGroupName, workGroup);
        plugin.getChannelConfiguration().readyToSave();

        user.sendMessage("成功为频道「" + channel.getName() + "」添加了工作组「" + workGroupName + "」");
    }

    @Filter(Words.REMOVE + Words.CHANNEL + Words.WORK_GROUP + " {频道} {工作组}")
    @Filter(Words.REMOVE + Words.WORK_GROUP + " {频道} {工作组}")
    @Required("xmmc.admin.channel.config")
    void removeWorkGroup(XiaoMingUser user, @FilterParameter("频道") Channel channel, @FilterParameter("工作组") WorkGroup workGroup) {
        channel.getWorkGroups().remove(workGroup.getName());
        plugin.getChannelConfiguration().readyToSave();

        user.sendMessage("成功删除频道「" + channel.getName() + "」的工作组「" + workGroup.getName() + "」");
    }

    @Filter(Words.CHANNEL + Words.WORK_GROUP + " {频道}")
    @Required("xmmc.admin.channel.look")
    void channelWorkGroupInfo(XiaoMingUser user, @FilterParameter("频道") Channel channel) {
        final Map<String, WorkGroup> workGroups = channel.getWorkGroups();
        if (workGroups.isEmpty()) {
            user.sendError("频道「" + channel.getName() + "」没有任何工作组");
            return;
        }

        user.sendMessage("「频道工作组信息」\n" +
                CollectionUtil.toIndexString(workGroups.entrySet(), x -> {
                    final WorkGroup workGroup = x.getValue();
                    final String description = Trigger.TRIGGER_NAMES.get(workGroup.getTrigger().getClass()) + " => " +
                            Optional.ofNullable(CollectionUtil.toString(workGroup.getExecutors(), y -> Executor.EXECUTOR_NAMES.get(y.getClass()), " -> ")).orElse("（无执行器）");
                    return description;
                })
        );
    }

    @Filter(Words.REMOVE + Words.WORK_GROUP + Words.EXECUTOR + " {频道} {工作组} {执行器序号}")
    @Filter(Words.REMOVE + Words.CHANNEL + Words.WORK_GROUP + Words.EXECUTOR + " {频道} {工作组} {执行器序号}")
    @Required("xmmc.admin.channel.config")
    void removeWorkGroupExecutor(XiaoMingUser user,
                                 @FilterParameter("频道") Channel channel,
                                 @FilterParameter("工作组") WorkGroup workGroup,
                                 @FilterParameter("执行器序号") int originalIndex) {
        final int index = originalIndex - 1;
        final List<Executor> executors = workGroup.getExecutors();
        if (!Indexs.isLegal(originalIndex, executors.size())) {
            user.sendError("执行器序号「" + originalIndex + "」错误，应该在 1 到 " + executors.size() + " 之间");
            return;
        }

        final Executor executor = executors.remove(index);
        plugin.getChannelConfiguration().readyToSave();

        if (executors.isEmpty()) {
            user.sendMessage("成功删除工作组的第 " + originalIndex + " 个执行器（" + Executor.EXECUTOR_NAMES.get(executor.getClass()) + "），现在该工作组没有任何执行器");
        } else {
            user.sendMessage("成功删除工作组的第 " + originalIndex + " 个执行器（" + Executor.EXECUTOR_NAMES.get(executor.getClass()) + "）");
        }
    }

    @Filter(Words.ADD + Words.WORK_GROUP + Words.EXECUTOR + " {频道} {工作组}")
    @Filter(Words.NEW + Words.WORK_GROUP + Words.EXECUTOR + " {频道} {工作组}")
    @Required("xmmc.admin.channel.config")
    void addWorkGroupExecutor(XiaoMingUser user,
                              @FilterParameter("频道") Channel channel,
                              @FilterParameter("工作组") WorkGroup workGroup) {
        user.sendMessage("输入序号，选择你要创建的执行器类型吧！");

        final Executor executor;
        final List<Class<?>> executorClasses = Collections.asUnmodifiableList(Executor.EXECUTOR_NAMES.keySet());
        while (true) {
            final Class<?> executorClass = Interactors.indexChooser(user, executorClasses, Executor.EXECUTOR_NAMES::get, 20);
            final Function<XiaoMingUser, Executor> guider = executorConfigurers.get(executorClass);
            if (guider == null) {
                user.sendError("当前版本插件并未设计该类型执行器的向导，重新选择工作组执行器类型吧！");
            } else {
                executor = guider.apply(user);
                break;
            }
        }

        workGroup.getExecutors().add(executor);
        plugin.getChannelConfiguration().readyToSave();
        user.sendMessage("成功为频道「" + channel.getName() + "」的工作组「" + workGroup.getName() + "」添加了一个执行器（" + Executor.EXECUTOR_NAMES.get(executor.getClass()) + "）");
    }

    @Filter(Words.INSERT + Words.WORK_GROUP + Words.EXECUTOR + " {频道} {工作组} {插入位置}")
    @Filter(Words.INSERT + Words.CHANNEL + Words.WORK_GROUP + Words.EXECUTOR + " {频道} {工作组} {插入位置}")
    @Required("xmmc.admin.channel.config")
    void insertWorkGroupExecutor(XiaoMingUser user,
                                 @FilterParameter("频道") Channel channel,
                                 @FilterParameter("工作组") WorkGroup workGroup,
                                 @FilterParameter("插入位置") int originalIndex) {
        final int index = originalIndex - 1;
        final List<Executor> executors = workGroup.getExecutors();
        if (index < 0 || index > executors.size()) {
            user.sendError("要插入的执行器位置 " + originalIndex + " 错误，应该在 1 到 " + (executors.size() + 1) + " 之间");
            return;
        }

        user.sendMessage("输入序号，选择你要插入的执行器类型吧！");

        final Executor executor;
        final List<Class<?>> executorClasses = Collections.asUnmodifiableList(Executor.EXECUTOR_NAMES.keySet());
        while (true) {
            final Class<?> executorClass = Interactors.indexChooser(user, executorClasses, Executor.EXECUTOR_NAMES::get, 20);
            final Function<XiaoMingUser, Executor> guider = executorConfigurers.get(executorClass);
            if (guider == null) {
                user.sendError("当前版本插件并未设计该类型执行器的向导，重新选择工作组执行器类型吧！");
            } else {
                executor = guider.apply(user);
                break;
            }
        }

        executors.add(index, executor);
        plugin.getChannelConfiguration().readyToSave();
        user.sendMessage("成功在频道「" + channel.getName() + "」的工作组「" + workGroup.getName() + "」的第 " + originalIndex + " 个执行器前插入了一个新的执行器（" + Executor.EXECUTOR_NAMES.get(executor.getClass()) + "）");
    }

    @Filter(Words.SET + Words.WORK_GROUP + Words.EXECUTOR + " {频道} {工作组} {执行器位置}")
    @Filter(Words.SET + Words.CHANNEL + Words.WORK_GROUP + Words.EXECUTOR + " {频道} {工作组} {执行器位置}")
    @Required("xmmc.admin.channel.config")
    void setWorkGroupExecutor(XiaoMingUser user,
                              @FilterParameter("频道") Channel channel,
                              @FilterParameter("工作组") WorkGroup workGroup,
                              @FilterParameter("执行器位置") int originalIndex) {
        final int index = originalIndex - 1;
        final List<Executor> executors = workGroup.getExecutors();
        if (Indexs.isLegal(index, executors.size())) {
            user.sendError("执行器位置 " + originalIndex + " 错误，应该在 1 到 " + executors.size() + " 之间");
            return;
        }

        final Executor elderExecutor = executors.get(index);

        user.sendMessage("输入序号，选择你要设置的执行器类型吧！");

        final Executor executor;
        final List<Class<?>> executorClasses = Collections.asUnmodifiableList(Executor.EXECUTOR_NAMES.keySet());
        while (true) {
            final Class<?> executorClass = Interactors.indexChooser(user, executorClasses, Executor.EXECUTOR_NAMES::get, 20);
            final Function<XiaoMingUser, Executor> guider = executorConfigurers.get(executorClass);
            if (guider == null) {
                user.sendError("当前版本插件并未设计该类型执行器的向导，重新选择工作组执行器类型吧！");
            } else {
                executor = guider.apply(user);
                break;
            }
        }

        executors.add(index, executor);
        plugin.getChannelConfiguration().readyToSave();
        user.sendMessage("成功在频道「" + channel.getName() + "」的工作组「" + workGroup.getName() + "」的第 " + originalIndex + " 个执行器修改为新的执行器（" + Executor.EXECUTOR_NAMES.get(executor.getClass()) + "）");
    }
}