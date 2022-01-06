package cn.chuanwise.xiaoming.minecraft.xiaoming.interactors;

import cn.chuanwise.toolkit.container.Container;
import cn.chuanwise.util.*;
import cn.chuanwise.xiaoming.annotation.Filter;
import cn.chuanwise.xiaoming.annotation.FilterParameter;
import cn.chuanwise.xiaoming.annotation.Required;
import cn.chuanwise.xiaoming.interactor.SimpleInteractors;
import cn.chuanwise.xiaoming.minecraft.protocol.CommandRequestResponse;
import cn.chuanwise.xiaoming.minecraft.xiaoming.Plugin;
import cn.chuanwise.xiaoming.minecraft.xiaoming.channel.scope.GroupScope;
import cn.chuanwise.xiaoming.minecraft.xiaoming.channel.scope.PrivateScope;
import cn.chuanwise.xiaoming.minecraft.xiaoming.channel.scope.QQScope;
import cn.chuanwise.xiaoming.minecraft.xiaoming.channel.scope.Scope;
import cn.chuanwise.xiaoming.minecraft.xiaoming.configuration.PlayerInfo;
import cn.chuanwise.xiaoming.minecraft.xiaoming.configuration.PluginConfiguration;
import cn.chuanwise.xiaoming.minecraft.xiaoming.configuration.StringGenerator;
import cn.chuanwise.xiaoming.minecraft.xiaoming.net.OnlineClient;
import cn.chuanwise.xiaoming.minecraft.xiaoming.util.Words;
import cn.chuanwise.xiaoming.user.XiaomingUser;
import lombok.Data;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class CommandRequestInteractors extends SimpleInteractors<Plugin> {
    @Override
    public void onRegister() {
        xiaomingBot.getInteractorManager().registerParameterParser(CommandRequestContext.class, context -> {
            final XiaomingUser user = context.getUser();
            final String inputValue = context.getInputValue();

            if (commandRequestContexts.isEmpty()) {
                user.sendError("目前没有任何待处理的指令请求");
                return null;
            }
            if (commandRequestContexts.size() == 1) {
                final CommandRequestContext requestContext = commandRequestContexts.values().iterator().next();
                if (StringUtil.notEmpty(inputValue)) {
                    if (Objects.equals(requestContext.verifyCode, inputValue)) {
                        user.sendMessage("目前只有一个指令请求，可以省略请求验证码哦");
                    } else {
                        user.sendMessage("目前只有一个指令请求，可以省略请求验证码哦（而且你打错啦，目前唯一的请求的验证码是「" + requestContext.verifyCode + "」）");
                    }
                }
                return Container.of(requestContext);
            }

            if (StringUtil.isEmpty(inputValue)) {
                user.sendError("目前有多个指令请求，必须指定请求验证码");
                return null;
            }
            final CommandRequestContext requestContext = commandRequestContexts.get(inputValue);
            if (Objects.isNull(requestContext)) {
                user.sendError("没有找到验证码为「" + inputValue + "」的指令请求");
                return null;
            }

            return Container.of(requestContext);
        }, true, plugin);
    }

    @Data
    public class CommandRequestContext {
        final Object condition = this;

        final OnlineClient onlineClient;
        final String playerName;
        final String worldName;
        final String command;
        final String verifyCode;

        final long requestTime = System.currentTimeMillis();

        volatile CommandRequestResponse response;

        boolean isOperated() {
            return Objects.nonNull(response);
        }

        boolean operate(CommandRequestResponse response) {
            if (isOperated()) {
                return false;
            }
            this.response = response;
            synchronized (condition) {
                condition.notifyAll();
            }
            return true;
        }

        String getDescription() {
            final Optional<PlayerInfo> optionalPlayerInfo = plugin.getPlayerConfiguration().getPlayerInfo(getPlayerName());

            return "「" + onlineClient.getServerInfo().getName() + "」中的玩家「" + playerName + "」" +
                    optionalPlayerInfo.map(x -> "（绑定了 " + CollectionUtil.toString(x.getAccountCodes(), xiaomingBot.getAccountManager()::getAliasOrCode) + "）").orElse("") +
                    "申请在世界「" + worldName + "」执行指令：" + command + "。请求验证码：" + verifyCode + "。";
        }

        CommandRequestResponse get(long timeout) throws InterruptedException {
            if (isOperated()) {
                return response;
            }

            if (ObjectUtil.wait(condition, timeout)) {
                return response;
            } else {
                response = CommandRequestResponse.Timeout.getInstance();
                synchronized (condition) {
                    condition.notifyAll();
                }
                return response;
            }
        }
    }
    final Map<String, CommandRequestContext> commandRequestContexts = new ConcurrentHashMap<>();

    public boolean isAllowCommandRequest() {
        return plugin.getPluginConfiguration().getCommandRequest().isEnabled();
    }

    public Optional<CommandRequestContext> newCommandRequest(OnlineClient onlineClient, String playerName, String worldName, String command) {
        final PluginConfiguration.CommandRequest commandRequest = plugin.getPluginConfiguration().getCommandRequest();
        ConditionUtil.checkState(commandRequest.isEnabled(), "command request hadn't enabled");

        final StringGenerator verifyCodeGenerator = commandRequest.getVerifyCode();
        String tempVerifyCode = StringUtil.randomString(verifyCodeGenerator.getCharacters(), verifyCodeGenerator.getLength());
        int generateCount = 1;
        while (Objects.nonNull(commandRequestContexts.get(tempVerifyCode)) && generateCount < verifyCodeGenerator.getMaxGenerateCount()) {
            tempVerifyCode = StringUtil.randomString(verifyCodeGenerator.getCharacters(), verifyCodeGenerator.getLength());
        }
        if (generateCount == verifyCodeGenerator.getMaxGenerateCount()) {
            commandRequest.getScopes().forEach(x -> x.sendMessage(Collections.singletonList("有新的指令申请请求，但无法再分配验证码")));
            return Optional.empty();
        }
        final String verifyCode = tempVerifyCode;

        final CommandRequestContext context = new CommandRequestContext(onlineClient, playerName, worldName, command, tempVerifyCode);
        xiaomingBot.getScheduler().run(() -> {
            try {
                commandRequestContexts.put(verifyCode, context);

                // 频道发出广播
                final long timeout = commandRequest.getTimeout();
                final String timeoutLength = TimeUtil.toTimeLength(timeout);

                final String broadcast;
                if (commandRequestContexts.size() == 1) {
                    broadcast = context.getDescription() + "\n" +
                            "请执行「批准指令请求」，或「拒绝指令请求」。\n" +
                            "此请求将在" + timeoutLength + "后自动取消。";
                } else {
                    broadcast = context.getDescription() + "\n" +
                            "请执行「批准指令请求  " + context.getVerifyCode() + "」，或「拒绝指令请求  " + verifyCode + "」。\n" +
                            "此请求将在" + timeoutLength + "后自动取消。" +
                            "因为目前有多个待处理的指令请求，因此处理指令请求时需要带上其验证码。";
                }
                final List<String> messages = Collections.singletonList(broadcast);
                commandRequest.getScopes().forEach(x -> x.sendMessage(messages));

                // 等待处理结果
                final CommandRequestResponse response = context.get(timeout);

                if (response instanceof CommandRequestResponse.Operated) {
                    final CommandRequestResponse.Operated operated = (CommandRequestResponse.Operated) response;
                    final String message = "验证码为「" + verifyCode + "」的指令请求被" + xiaomingBot.getAccountManager().getAliasAndCode(operated.getOperatorCode()) +
                            (operated.isAccepted() ? "批准" : "拒绝");

                    final List<String> receipt = Collections.singletonList(message);
                    commandRequest.getScopes().forEach(x -> x.sendMessage(receipt));
                    return;
                }
                if (response instanceof CommandRequestResponse.Timeout) {
                    final String message = "验证码为「" + verifyCode + "」的指令请求未被及时处理，已自动取消";
                    final List<String> receipt = Collections.singletonList(message);
                    commandRequest.getScopes().forEach(x -> x.sendMessage(receipt));
                    return;
                }

                throw new IllegalStateException();
            } catch (InterruptedException exception) {
                final List<String> messages = Collections.singletonList("验证码为「" + verifyCode+ "」的指令请求过程被打断");
                commandRequest.getScopes().forEach(x -> x.sendMessage(messages));
                plugin.getLogger().info("等待指令请求结果被打断");
            } finally {
                commandRequestContexts.remove(verifyCode);
                if (!context.isOperated()) {
                    context.operate(CommandRequestResponse.Timeout.getInstance());
                }
            }
        });
        return Optional.of(context);
    }

    @Filter(Words.COMMAND + Words.REQUEST)
    @Required("xmmc.admin.command.request.list")
    void listCommandRequest(XiaomingUser user) {
        if (commandRequestContexts.isEmpty()) {
            user.sendError("目前没有任何待处理的指令请求");
            return;
        }
        if (commandRequestContexts.size() == 1) {
            final CommandRequestContext context = commandRequestContexts.values().iterator().next();
            user.sendMessage("目前唯一的指令请求信息：" + context.getDescription() + "\n" +
                    "（已等待 " + TimeUtil.toTimeLength(System.currentTimeMillis() - context.requestTime) + " ）");
            return;
        }

        user.sendMessage("「指令请求消息」\n" +
                CollectionUtil.toIndexString(commandRequestContexts.values(), x -> x.getDescription() +
                        "（已等待 " + TimeUtil.toTimeLength(System.currentTimeMillis() - x.requestTime) + " ）"));
    }

    @Filter(Words.ACCEPT + Words.COMMAND + Words.REQUEST)
    @Required("xmmc.admin.command.request.accept")
    void acceptOnlyCommandRequest(XiaomingUser user, @FilterParameter("验证码") CommandRequestContext context) {
        context.operate(new CommandRequestResponse.Operated(user.getCode(), true));
        user.sendMessage("已批准目前唯一的指令请求");
    }

    @Filter(Words.DENY + Words.COMMAND + Words.REQUEST)
    @Required("xmmc.admin.command.request.deny")
    void denyOnlyCommandRequest(XiaomingUser user, @FilterParameter("验证码") CommandRequestContext context) {
        context.operate(new CommandRequestResponse.Operated(user.getCode(), false));
        user.sendMessage("已拒绝目前唯一的指令请求");
    }

    @Filter(Words.ACCEPT + Words.COMMAND + Words.REQUEST + " {验证码}")
    @Required("xmmc.admin.command.request.accept")
    void acceptCommandRequest(XiaomingUser user, @FilterParameter("验证码") CommandRequestContext context) {
        context.operate(new CommandRequestResponse.Operated(user.getCode(), true));
        user.sendMessage("已批准指令请求「" + context.getVerifyCode() + "」");
    }

    @Filter(Words.DENY + Words.COMMAND + Words.REQUEST + " {验证码}")
    @Required("xmmc.admin.command.request.deny")
    void denyCommandRequest(XiaomingUser user, @FilterParameter("验证码") CommandRequestContext context) {
        context.operate(new CommandRequestResponse.Operated(user.getCode(), false));
        user.sendMessage("已拒绝指令请求「" + context.getVerifyCode() + "」");
    }

    @Filter(Words.COMMAND + Words.REQUEST + Words.INFO)
    @Required("xmmc.admin.command.request.info")
    void commandRequestInfo(XiaomingUser user) {
        final PluginConfiguration.CommandRequest commandRequest = plugin.getPluginConfiguration().getCommandRequest();
        if (commandRequest.isEnabled()) {
            user.sendMessage("指令请求已开启，请求将被发送到：" + CollectionUtil.toString(commandRequest.getScopes(), Scope::getDescription));
        } else {
            user.sendMessage("指令请求尚未开启");
        }
    }

    @Filter(Words.ENABLE + Words.COMMAND + Words.REQUEST)
    @Required("xmmc.admin.command.request.enable")
    void enableCommandRequest(XiaomingUser user) {
        final PluginConfiguration pluginConfiguration = plugin.getPluginConfiguration();
        final PluginConfiguration.CommandRequest commandRequest = pluginConfiguration.getCommandRequest();
        if (commandRequest.isEnabled()) {
            user.sendMessage("指令请求已开启，无需重复开启");
        } else {
            final List<QQScope> scopes = commandRequest.getScopes();
            commandRequest.setEnabled(true);
            if (scopes.isEmpty()) {
                user.sendMessage("指令请求已启动，但请求将不会被发送到任何范围。\n" +
                        "请使用「添加指令请求群聊范围  [群标签]」或「添加指令请求私聊范围  [用户标签]」设置请求来临时的通知区域。");
            } else {
                user.sendMessage("指令请求已启动，请求将被发送到：" + CollectionUtil.toString(scopes, Scope::getDescription));
            }
            pluginConfiguration.readyToSave();
        }
    }

    @Filter(Words.ENABLE + Words.COMMAND + Words.REQUEST)
    @Required("xmmc.admin.command.request.disable")
    void disableCommandRequest(XiaomingUser user) {
        final PluginConfiguration pluginConfiguration = plugin.getPluginConfiguration();
        final PluginConfiguration.CommandRequest commandRequest = pluginConfiguration.getCommandRequest();
        if (commandRequest.isEnabled()) {
            commandRequest.setEnabled(false);
            user.sendMessage("指令请求已关闭");
            pluginConfiguration.readyToSave();
        } else {
            final List<QQScope> scopes = commandRequest.getScopes();
            user.sendMessage("指令请求尚未开启，或并不会工作");
        }
    }

    @Filter(Words.ADD + Words.COMMAND + Words.REQUEST + Words.GROUP + Words.SCOPE + " {群标签}")
    @Required("xmmc.admin.command.request.scope.add")
    void addCommandRequestGroupScope(XiaomingUser user, @FilterParameter("群标签") String groupTag) {
        final PluginConfiguration pluginConfiguration = plugin.getPluginConfiguration();
        final PluginConfiguration.CommandRequest commandRequest = pluginConfiguration.getCommandRequest();

        commandRequest.getScopes().add(new GroupScope(groupTag));
        if (commandRequest.isEnabled()) {
            user.sendMessage("成功为指令请求增加了群聊通知范围");
        } else {
            user.sendMessage("成功为指令请求增加了群聊通知范围，但指令请求并未开启");
        }
        pluginConfiguration.readyToSave();
    }

    @Filter(Words.ADD + Words.COMMAND + Words.REQUEST + Words.PRIVATE + Words.SCOPE + " {用户标签}")
    @Required("xmmc.admin.command.request.scope.add")
    void addCommandRequestPrivateScope(XiaomingUser user, @FilterParameter("用户标签") String accountTag) {
        final PluginConfiguration pluginConfiguration = plugin.getPluginConfiguration();
        final PluginConfiguration.CommandRequest commandRequest = pluginConfiguration.getCommandRequest();

        commandRequest.getScopes().add(new PrivateScope(accountTag));
        if (commandRequest.isEnabled()) {
            user.sendMessage("成功为指令请求增加了私聊通知范围");
        } else {
            user.sendMessage("成功为指令请求增加了私聊通知范围，但指令请求并未开启");
        }
        pluginConfiguration.readyToSave();
    }

    @Filter(Words.REMOVE + Words.COMMAND + Words.REQUEST + Words.GROUP + Words.SCOPE + " {群标签}")
    @Required("xmmc.admin.command.request.scope.remove")
    void removeCommandRequestGroupScope(XiaomingUser user, @FilterParameter("群标签") String groupTag) {
        final PluginConfiguration pluginConfiguration = plugin.getPluginConfiguration();
        final PluginConfiguration.CommandRequest commandRequest = pluginConfiguration.getCommandRequest();

        commandRequest.getScopes().remove(new GroupScope(groupTag));
        user.sendMessage("成功删除了指令请求的群聊通知范围");
        pluginConfiguration.readyToSave();
    }

    @Filter(Words.REMOVE + Words.COMMAND + Words.REQUEST + Words.PRIVATE + Words.SCOPE + " {用户标签}")
    @Required("xmmc.admin.command.request.scope.remove")
    void removeCommandRequestPrivateScope(XiaomingUser user, @FilterParameter("用户标签") String accountTag) {
        final PluginConfiguration pluginConfiguration = plugin.getPluginConfiguration();
        final PluginConfiguration.CommandRequest commandRequest = pluginConfiguration.getCommandRequest();

        commandRequest.getScopes().remove(new PrivateScope(accountTag));
        user.sendMessage("成功删除了指令请求的私聊通知范围");
        pluginConfiguration.readyToSave();
    }
}
