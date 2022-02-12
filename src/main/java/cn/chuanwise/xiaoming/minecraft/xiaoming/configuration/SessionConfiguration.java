package cn.chuanwise.xiaoming.minecraft.xiaoming.configuration;

import cn.chuanwise.xiaoming.minecraft.xiaoming.XMMCXiaoMingPlugin;
import cn.chuanwise.xiaoming.preservable.SimplePreservable;
import lombok.Data;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Data
public class SessionConfiguration extends SimplePreservable<XMMCXiaoMingPlugin> {
    @Data
    public static class Connection {
        boolean autoBind = true;

        int port = 23333;

        long responseTimeout = TimeUnit.SECONDS.toMillis(30);
        long responseDelay = TimeUnit.MILLISECONDS.toMillis(500);

        long verifyTimeout = TimeUnit.MINUTES.toMillis(5);

        int threadCount = 30;

        long idleTimeout = TimeUnit.SECONDS.toMillis(50);
    }
    Connection connection = new Connection();

    Map<String, ServerInfo> servers = new HashMap<>();

    public Set<ServerInfo> searchServerByTag(String serverTag) {
        return Collections.unmodifiableSet(servers.values()
                .stream()
                .filter(x -> x.hasTag(serverTag)).collect(Collectors.toSet()));
    }
}