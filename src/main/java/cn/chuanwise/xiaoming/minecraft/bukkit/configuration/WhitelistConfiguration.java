package cn.chuanwise.xiaoming.minecraft.bukkit.configuration;

import cn.chuanwise.storage.file.StoredFile;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class WhitelistConfiguration extends StoredFile {
    boolean enable;

    @Data
    public static class Operation {
        @Data
        public static class Command {
            boolean enable;

            List<String> console = new ArrayList<>();
            List<String> player = new ArrayList<>();
        }
        Command command = new Command();

        @Data
        public static class Message {
            boolean enable;
            List<String> messages = new ArrayList<>();
        }
        Message message = new Message();

        @Data
        public static class Kick {
            boolean enable;
            String message;
        }
        Kick kick = new Kick();
    }
    Operation botOffline = new Operation();
    Operation nonBind = new Operation();
    Operation error = new Operation();
    {
        // config default value
        botOffline.kick.enable = true;
        botOffline.kick.message = "§7[§6小明§7] §e服务器尚未连接小明，暂时不能进入服务器，请联系管理员";

        error.kick.enable = true;
        error.kick.message = "§7[§4小明§7] §c服务器和小明的连接出现错误，请稍后进入服务器重试。若仍出现该问题，请联系管理员";

        nonBind.kick.enable = true;
        nonBind.kick.message = "§7[§2小明§7] §a欢迎来到服务器！请在{timeout}内加入服务器 QQ 群 §e§l1028959718 §a发送§7「§b进服验证  {verifyCode}§7」§a以获得白名单";
    }
}
