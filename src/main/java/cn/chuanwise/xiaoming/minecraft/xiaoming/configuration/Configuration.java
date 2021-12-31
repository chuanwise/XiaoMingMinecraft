package cn.chuanwise.xiaoming.minecraft.xiaoming.configuration;

import cn.chuanwise.xiaoming.minecraft.xiaoming.channel.Channel;
import cn.chuanwise.xiaoming.minecraft.xiaoming.Plugin;
import cn.chuanwise.xiaoming.minecraft.xiaoming.channel.trigger.Trigger;
import cn.chuanwise.xiaoming.preservable.SimplePreservable;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Data
public class Configuration extends SimplePreservable<Plugin> {
    @Data
    public static class Connection {
        boolean autoBind = true;

        int port = 23333;

        long responseTimeout = TimeUnit.MINUTES.toMillis(1);
        long responseDelay = TimeUnit.MILLISECONDS.toMillis(500);

        long verifyTimeout = TimeUnit.MINUTES.toMillis(10);

        int threadCount = 10;
    }
    Connection connection = new Connection();

    Map<String, ServerInfo> servers = new HashMap<>();

    @Data
    public static class Generator {
        @Data
        @AllArgsConstructor
        @NoArgsConstructor
        public static class RandomStringGenerator {
            String characters = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
            int length = 10;
            int maxGenerateCount = 100;
        }

        RandomStringGenerator verifyCode = new RandomStringGenerator("0123456789", 4, 100);
        RandomStringGenerator password = new RandomStringGenerator("ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789!@#$%^&*()_+", 100, 100);
    }
    Generator generator = new Generator();

    Map<String, Trigger> triggers = new HashMap<>();
    Map<String, Channel> channels = new HashMap<>();
}