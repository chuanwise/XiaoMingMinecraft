package cn.chuanwise.xiaoming.minecraft.xiaoming.interactors;

import cn.chuanwise.util.StringUtil;
import cn.chuanwise.util.Times;
import cn.chuanwise.xiaoming.annotation.Filter;
import cn.chuanwise.xiaoming.annotation.Required;
import cn.chuanwise.xiaoming.contact.message.Message;
import cn.chuanwise.xiaoming.exception.InteractExitedException;
import cn.chuanwise.xiaoming.exception.InteractInterrtuptedException;
import cn.chuanwise.xiaoming.exception.InteractTimeoutException;
import cn.chuanwise.xiaoming.interactor.SimpleInteractors;
import cn.chuanwise.xiaoming.minecraft.xiaoming.XMMCXiaoMingPlugin;
import cn.chuanwise.xiaoming.minecraft.xiaoming.configuration.BaseConfiguration;
import cn.chuanwise.xiaoming.minecraft.xiaoming.configuration.SessionConfiguration;
import cn.chuanwise.xiaoming.minecraft.xiaoming.configuration.ServerInfo;
import cn.chuanwise.xiaoming.minecraft.xiaoming.configuration.StringGenerator;
import cn.chuanwise.xiaoming.minecraft.xiaoming.net.Server;
import cn.chuanwise.xiaoming.minecraft.xiaoming.util.Words;
import cn.chuanwise.xiaoming.user.XiaoMingUser;
import lombok.Getter;

import java.util.Objects;
import java.util.Optional;

@Getter
@SuppressWarnings("all")
public class VerifyInteractors extends SimpleInteractors<XMMCXiaoMingPlugin> {
    /** 当前正在等待新连接接入的用户 */
    protected volatile XiaoMingUser strangeServerWaiter = null;

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
    void meeting(XiaoMingUser user) {
        final Server server = plugin.getServer();
        if (!server.isBound()) {
            user.sendError("服务器还没有启动，请先使用「启动服务器」启动吧");
            return;
        }

        if (Objects.isNull(strangeServerWaiter)) {
            strangeServerWaiter = user;
            user.sendMessage("成功在当前会话下允许新服务器接入。\n" +
                    "请在你的 Minecraft 服务器上执行 /xm connect 连接小明");
            meetingContext = null;
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
    void cancelWaiting(XiaoMingUser user) {
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

        final SessionConfiguration sessionConfiguration = plugin.getSessionConfiguration();
        final BaseConfiguration baseConfiguration = plugin.getBaseConfiguration();
        final StringGenerator verifyCodeGenerator = baseConfiguration.getGenerator().getVerifyCode();

        final String verifyCode = StringUtil.randomString(verifyCodeGenerator.getCharacters(), verifyCodeGenerator.getLength());
        meetingContext = new MeetingContext(verifyCode);

        // 启动询问线程
        xiaoMingBot.getScheduler().run(() -> {
            // 通知有人连接了
            final XiaoMingUser user = this.strangeServerWaiter;
            final long timeout = sessionConfiguration.getConnection().getVerifyTimeout();
            final String timeoutLength = Times.toTimeLength(timeout);

            user.sendMessage("有陌生服务器接入。\n" +
                    "如果是你的服务器，请在" + timeoutLength + "内告诉我服务器【后台】显示的连接验证码，\n" +
                    "回复其他任何内容小明都将拒绝建立连接");
            try {
                final Message message = user.nextMessageOrExit(timeout);
                if (Objects.equals(message.serialize(), verifyCode)) {
                    user.sendMessage("告诉我这个服务器的名字吧！");

                    String name = null;
                    while (true) {
                        name = user.nextMessageOrExit().serialize();
                        if (Objects.nonNull(sessionConfiguration.getServers().get(name))) {
                            user.sendError("已经有叫这个名字的服务器了，换个名字再输入一次吧！");
                        } else {
                            break;
                        }
                    }

                    final ServerInfo serverInfo = new ServerInfo();
                    serverInfo.setRegisterTimeMillis(System.currentTimeMillis());
                    serverInfo.setName(name);
                    sessionConfiguration.getServers().put(name, serverInfo);

                    // 生成随机密码
                    final StringGenerator passwordGenerator = baseConfiguration.getGenerator().getPassword();
                    final String password = StringUtil.randomString(passwordGenerator.getCharacters(), passwordGenerator.getLength());
                    serverInfo.setPassword(password);

                    meetingContext.accept(user.getCode(), serverInfo);
                    user.sendMessage("成功批准该服务器连接，并退出迎接新服务器的模式");
                    sessionConfiguration.readyToSave();
                } else {
                    user.sendError("已拒绝该服务器连接");
                    meetingContext.deny(user.getCode());
                }
            } catch (InteractExitedException | InteractInterrtuptedException exception) {
                user.sendError("操作被取消");
                meetingContext.deny(xiaoMingBot.getCode());
            } catch (InteractTimeoutException exception) {
                user.sendError("你没有及时回复，小明已拒绝该服务器连接");
                meetingContext.deny(xiaoMingBot.getCode());
            } finally {
                strangeServerWaiter = null;
                meetingContext = null;
            }
        });

        return Optional.of(meetingContext);
    }

    public boolean isAllowStrangeServerConnect() {
        return Objects.nonNull(strangeServerWaiter);
    }
}
