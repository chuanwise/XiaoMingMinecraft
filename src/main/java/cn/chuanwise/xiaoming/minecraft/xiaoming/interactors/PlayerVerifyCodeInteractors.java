package cn.chuanwise.xiaoming.minecraft.xiaoming.interactors;

import cn.chuanwise.common.util.CollectionUtil;
import cn.chuanwise.common.util.Times;
import cn.chuanwise.toolkit.container.Container;
import cn.chuanwise.xiaoming.annotation.Filter;
import cn.chuanwise.xiaoming.annotation.FilterParameter;
import cn.chuanwise.xiaoming.annotation.Required;
import cn.chuanwise.xiaoming.interactor.SimpleInteractors;
import cn.chuanwise.xiaoming.minecraft.xiaoming.XMMCXiaoMingPlugin;
import cn.chuanwise.xiaoming.minecraft.xiaoming.configuration.PlayerConfiguration;
import cn.chuanwise.xiaoming.minecraft.xiaoming.configuration.PlayerVerifyCodeConfiguration;
import cn.chuanwise.xiaoming.minecraft.xiaoming.util.Words;
import cn.chuanwise.xiaoming.user.XiaoMingUser;
import lombok.Getter;

import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;

public class PlayerVerifyCodeInteractors extends SimpleInteractors<XMMCXiaoMingPlugin> {
    @Override
    public void onRegister() {
        xiaoMingBot.getInteractorManager().registerParameterParser(PlayerVerifyCodeConfiguration.VerifyInfo.class, context -> {
            final XiaoMingUser user = context.getUser();
            final String inputValue = context.getInputValue();

            final PlayerVerifyCodeConfiguration.VerifyInfo verifyInfo = plugin.getPlayerVerifyCodeConfiguration()
                    .getVerifyInfo()
                    .get(inputValue);

            if (Objects.isNull(verifyInfo)) {
                user.sendError("验证码不存在，请仔细核对");
                return null;
            }
            return Container.of(verifyInfo);
        }, true, plugin);
    }

    @Filter(Words.COME_TO_SERVER + Words.VERIFY + " {验证码}")
    @Filter(Words.COME_TO_SERVER + " {验证码}")
    @Required("xmmc.user.verify")
    void verifyBind(XiaoMingUser user, @FilterParameter("验证码") PlayerVerifyCodeConfiguration.VerifyInfo verifyInfo) {
        final PlayerVerifyCodeConfiguration playerVerifyCodeConfiguration = plugin.getPlayerVerifyCodeConfiguration();
        final PlayerConfiguration playerConfiguration = plugin.getPlayerConfiguration();

        playerVerifyCodeConfiguration.getVerifyInfo().values().remove(verifyInfo);
        playerVerifyCodeConfiguration.readyToSave();

        final long delay = System.currentTimeMillis() - verifyInfo.getTimeMillis();
        if (delay > playerVerifyCodeConfiguration.getTimeout()) {
            user.sendError("验证码已失效，请重新进服获取");
            return;
        }

        // 检查这段时间用户名是否被绑定
        final String playerName = verifyInfo.getPlayerName();
        switch (playerConfiguration.forceBind(user.getCode(), verifyInfo.getPlayerName())) {
            case SUCCEED:
                playerConfiguration.readyToSave();
                user.sendMessage("成功绑定用户名「" + playerName + "」，这个账号可以进服了");
                break;
            case OTHER:
                user.sendError("验证码已失效，用户名「" + playerName + "」已被其他人绑定");
                break;
            case REPEAT:
                user.sendError("验证码已失效，用户名「" + playerName + "」已被你绑定");
                break;
            case DENIED:
            default:
                throw new NoSuchElementException();
        }
    }

    @Filter(Words.COME_TO_SERVER + Words.VERIFY_CODE + " {验证码}")
    @Required("xmmc.admin.verify")
    void verifyCodeInfo(XiaoMingUser user, @FilterParameter("验证码") PlayerVerifyCodeConfiguration.VerifyInfo verifyInfo) {
        user.sendMessage("「进服验证码信息」\n" +
                "申请时间：" + Times.format(verifyInfo.getTimeMillis()) + "\n" +
                "玩家名：" + verifyInfo.getPlayerName());
    }

    @Filter(Words.REMOVE + Words.COME_TO_SERVER + Words.VERIFY_CODE + " {验证码}")
    @Required("xmmc.admin.verify")
    void removeVerifyCode(XiaoMingUser user, @FilterParameter("验证码") PlayerVerifyCodeConfiguration.VerifyInfo verifyInfo) {
        final PlayerVerifyCodeConfiguration configuration = plugin.getPlayerVerifyCodeConfiguration();
        configuration.getVerifyInfo().values().remove(verifyInfo);
        configuration.readyToSave();

        user.sendMessage("成功删除玩家「" + verifyInfo.getPlayerName() + "」的进服验证码");
    }

    @Filter(Words.COME_TO_SERVER + Words.VERIFY_CODE)
    @Required("xmmc.admin.verify")
    void verifyCodes(XiaoMingUser user) {
        final PlayerVerifyCodeConfiguration configuration = plugin.getPlayerVerifyCodeConfiguration();
        final Map<String, PlayerVerifyCodeConfiguration.VerifyInfo> verifyInfo = configuration.getVerifyInfo();
        if (verifyInfo.isEmpty()) {
            user.sendMessage("目前无任何有效进服验证码");
        } else {
            user.sendMessage("「进服验证码表」\n" +
                    CollectionUtil.toString(verifyInfo.entrySet(), x -> x.getKey() + "：" + x.getValue().getPlayerName()
                            + (System.currentTimeMillis() - x.getValue().getTimeMillis() > configuration.getTimeout() ? "（已失效）" : "")));
        }
    }
}

@Getter
class Demo {
    String chuanwise = "hey!";
}

class F {
    void f() {
        final Demo demo = new Demo();
        final String chuanwise = demo.getChuanwise();
    }
}