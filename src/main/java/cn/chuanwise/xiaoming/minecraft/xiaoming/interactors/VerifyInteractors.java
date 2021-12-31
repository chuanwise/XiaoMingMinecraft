package cn.chuanwise.xiaoming.minecraft.xiaoming.interactors;

import cn.chuanwise.util.ConditionUtil;
import cn.chuanwise.util.StringUtil;
import cn.chuanwise.util.TimeUtil;
import cn.chuanwise.xiaoming.annotation.Filter;
import cn.chuanwise.xiaoming.annotation.Required;
import cn.chuanwise.xiaoming.contact.message.Message;
import cn.chuanwise.xiaoming.exception.InteractExitedException;
import cn.chuanwise.xiaoming.exception.InteractInterrtuptedException;
import cn.chuanwise.xiaoming.exception.InteractTimeoutException;
import cn.chuanwise.xiaoming.interactor.SimpleInteractors;
import cn.chuanwise.xiaoming.minecraft.xiaoming.Plugin;
import cn.chuanwise.xiaoming.minecraft.xiaoming.configuration.Configuration;
import cn.chuanwise.xiaoming.minecraft.xiaoming.configuration.ServerInfo;
import cn.chuanwise.xiaoming.minecraft.xiaoming.util.Words;
import cn.chuanwise.xiaoming.user.XiaomingUser;
import lombok.Getter;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Getter
public class VerifyInteractors extends SimpleInteractors<Plugin> {
    /** 当前正在等待新连接接入的用户 */
    protected volatile XiaomingUser strangeServerWaiter = null;

    /** 新连接上下文 */
    @Getter
    public static class MeetingContext {
        protected final String verifyCode;
        protected final Object condition = this;

        protected volatile Long operatorCode;
        protected volatile ServerInfo serverInfo;

        public MeetingContext(String verifyCode) {
            this.verifyCode = verifyCode;
        }

        public boolean isHandled() {
            return Objects.nonNull(operatorCode);
        }

        public boolean accept(long operatorCode, ServerInfo serverInfo) {
            if (isHandled()) {
                return false;
            }

            this.operatorCode = operatorCode;
            this.serverInfo = serverInfo;
            synchronized (condition) {
                condition.notifyAll();
            }

            return true;
        }

        public boolean deny(long operatorCode) {
            if (isHandled()) {
                return false;
            }

            this.operatorCode = operatorCode;
            synchronized (condition) {
                condition.notifyAll();
            }

            return true;
        }

        public boolean isAccepted() {
            return isHandled() && Objects.nonNull(serverInfo);
        }

        public boolean isDenied() {
            return isHandled() && Objects.isNull(serverInfo);
        }
    }
    protected volatile MeetingContext meetingContext;

    /** 是否正在处理新的连接 */
    public boolean isWaiterBusy() {
        return Objects.nonNull(meetingContext);
    }

    @Filter(Words.MEET + Words.NEW + Words.SERVER)
    @Filter(Words.ALLOW + Words.NEW + Words.SERVER)
    @Required("xmmc.admin.meeting.enable")
    void waiting(XiaomingUser user) {
        if (Objects.isNull(strangeServerWaiter)) {
            strangeServerWaiter = user;
        } else {
            if (strangeServerWaiter.getCode() == user.getCode()) {
                if (strangeServerWaiter == user) {
                    user.sendError("你已经在当前会话下允许新服务器接入了哦");
                } else {
                    user.sendError("你已经在其他会话下允许新服务器接入了哦");
                }
            } else {
                user.sendError(strangeServerWaiter.getAliasAndCode() + " 正在迎接新服务器接入了哦。可以使用「取消迎接新服务器」");
            }
        }
    }

    @Filter(Words.CANCEL + Words.MEET + Words.NEW + Words.SERVER)
    @Required("xmmc.admin.meeting.cancel")
    void cancelWaiting(XiaomingUser user) {
        if (Objects.isNull(strangeServerWaiter)) {
            user.sendError("目前并没有任何人在迎接新服务器接入哦");
            return;
        }

        final long operatorCode = user.getCode();
        if (strangeServerWaiter.getCode() == operatorCode) {
            if (strangeServerWaiter == user) {
                user.sendError("已停止你在当前会话中迎接新服务器");
            } else {
                user.sendError("已停止你在其他会话中迎接新服务器");
            }
        } else {
            user.sendError("已停止 " + strangeServerWaiter.getAliasAndCode() + " 迎接新服务器");
            strangeServerWaiter.sendWarning(user.getAliasAndCode() + " 强制终止了你迎接新服务器");
        }
        strangeServerWaiter = null;

        // 拒绝剩下的服务器
        if (Objects.nonNull(meetingContext)) {
            meetingContext.deny(user.getCode());
        }
    }

    @SuppressWarnings("all")
    public Optional<MeetingContext> onMeetingActive() {
        // 如果正在忙碌，返回 empty
        if (Objects.nonNull(meetingContext)) {
            return Optional.empty();
        }

        final Configuration configuration = plugin.getConfiguration();
        final Configuration.Generator.RandomStringGenerator verifyCodeGenerator = configuration.getGenerator().getVerifyCode();

        final String verifyCode = StringUtil.randomString(verifyCodeGenerator.getCharacters(), verifyCodeGenerator.getLength());
        meetingContext = new MeetingContext(verifyCode);

        // 启动询问线程
        xiaomingBot.getScheduler().run(() -> {
            // 通知有人连接了
            final XiaomingUser user = this.strangeServerWaiter;
            final long timeout = configuration.getConnection().getVerifyTimeout();
            final String timeoutLength = TimeUtil.toTimeLength(timeout);

            user.sendMessage("有新的服务器接入，这是你的服务器吗？\n" +
                    "连接验证码：" + meetingContext.getVerifyCode() + "（你可以在服务器后台找到）。\n" +
                    "在" + timeoutLength + "内回复「是」，小明将批准连接。" +
                    "其他任何回答时小明将拒绝连接");
            try {
                final Message message = user.nextMessageOrExit(timeout);
                if (Objects.equals(message.serialize(), "是")) {
                    user.sendMessage("给这个服务器起一个名字吧！");

                    String name = null;
                    while (true) {
                        name = user.nextMessageOrExit().serialize();
                        if (Objects.nonNull(configuration.getServers().get(name))) {
                            user.sendError("已经有叫这个名字的服务器了，换个名字再输入一次吧！");
                        } else {
                            break;
                        }
                    }

                    final ServerInfo serverInfo = new ServerInfo();
                    serverInfo.setName(name);
                    configuration.getServers().put(name, serverInfo);

                    // 生成随机密码
                    final Configuration.Generator.RandomStringGenerator passwordGenerator = configuration.getGenerator().getPassword();
                    final String password = StringUtil.randomString(passwordGenerator.getCharacters(), passwordGenerator.getLength());
                    serverInfo.setPassword(password);

                    meetingContext.accept(user.getCode(), serverInfo);
                    user.sendMessage("成功批准该服务器连接，并退出迎接新服务器的模式");
                    strangeServerWaiter = null;
                }
            } catch (InteractExitedException | InteractInterrtuptedException exception) {
                user.sendError("操作被取消");
                meetingContext.deny(xiaomingBot.getCode());
            } catch (InteractTimeoutException exception) {
                user.sendError("你没有及时回复，小明已拒绝该服务器连接。");
                meetingContext.deny(xiaomingBot.getCode());
            }
        });

        return Optional.of(meetingContext);
    }

    public boolean isAllowStrangeServerConnect() {
        return Objects.nonNull(strangeServerWaiter);
    }
}
