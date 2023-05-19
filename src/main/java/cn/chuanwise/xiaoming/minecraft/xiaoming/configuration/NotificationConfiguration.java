package cn.chuanwise.xiaoming.minecraft.xiaoming.configuration;

import cn.chuanwise.xiaoming.minecraft.xiaoming.XMMCXiaoMingPlugin;
import cn.chuanwise.xiaoming.preservable.SimplePreservable;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
public class NotificationConfiguration extends SimplePreservable<XMMCXiaoMingPlugin> {
    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class NotificationInfo {
        @Data
        @AllArgsConstructor
        @NoArgsConstructor
        public static class Title {
            boolean enable;

            String title;
            String subTitle;
            long fadeIn = 20, delay = 70, fadeOut = 30;

            public Title(boolean enable, String title, String subTitle) {
                this.enable = enable;
                this.title = title;
                this.subTitle = subTitle;
            }
        }
        Title title;

        @Data
        @AllArgsConstructor
        @NoArgsConstructor
        public static class Message {
            boolean enable;

            String message;
        }
        Message message;

        String placeHolder;
    }
    NotificationInfo at = new NotificationInfo(
            new NotificationInfo.Title(false, "§e有人 §c@ §e你", "§a请注意§7「§e{communicator().alias}」§a中的消息"),
            new NotificationInfo.Message(true, "§7[§6有人 §e@ §6你§7] §e{communicator().alias} §8| §e{user.alias} §8§l: §e{message}"),
            "@{target.alias} "
    );

    NotificationInfo atAll = new NotificationInfo(
            new NotificationInfo.Title(true, "§c§l@ §c全体成员", "§c请注意§7「§e{communicator().alias}」§c中的消息"),
            new NotificationInfo.Message(true, "§7[§c@ §4全体成员§7] §4{communicator().alias} §8| §c{user.alias} §8§l: §c{message}"),
            "@全体成员 "
    );
}
