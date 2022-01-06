package cn.chuanwise.xiaoming.minecraft.xiaoming.configuration;

import cn.chuanwise.xiaoming.minecraft.xiaoming.Plugin;
import cn.chuanwise.xiaoming.minecraft.xiaoming.channel.scope.QQScope;
import cn.chuanwise.xiaoming.minecraft.xiaoming.channel.scope.Scope;
import cn.chuanwise.xiaoming.preservable.SimplePreservable;
import lombok.Data;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Data
public class PluginConfiguration extends SimplePreservable<Plugin> {
    boolean debug = false;

    @Data
    public static class Connection {
        boolean autoBind = true;

        int port = 23333;

        long responseTimeout = TimeUnit.MINUTES.toMillis(1);
        long responseDelay = TimeUnit.MILLISECONDS.toMillis(500);

        long verifyTimeout = TimeUnit.MINUTES.toMillis(5);

        int threadCount = 30;
        long checkActivePeriod = TimeUnit.SECONDS.toMillis(30);

        long idleTimeout = TimeUnit.SECONDS.toMillis(50);
    }
    Connection connection = new Connection();

    Map<String, ServerInfo> servers = new HashMap<>();

    @Data
    public static class Generators {
        StringGenerator verifyCode = new StringGenerator("0123456789", 4, 100);
        StringGenerator password = new StringGenerator("ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789!@#$%^&*()_+", 100, 100);
    }
    Generators generator = new Generators();

    @Data
    public static class CommandRequest {
        boolean enabled = false;
        List<QQScope> scopes = new ArrayList<>();
        StringGenerator verifyCode = new StringGenerator("0123456789", 4, 100);
        long timeout = TimeUnit.MINUTES.toMillis(5);

        public boolean isEnabled() {
            return enabled && !scopes.isEmpty();
        }
    }
    CommandRequest commandRequest = new CommandRequest();
}