package cn.chuanwise.xiaoming.minecraft.bukkit;

import cn.chuanwise.storage.file.StoredFile;
import lombok.Data;

import java.util.concurrent.TimeUnit;

@Data
public class Configuration extends StoredFile {
    boolean debug = false;

    @Data
    public static class Connection {
        boolean autoConnect = true;

        String host = "127.0.0.1";

        int port = 23333;

        int maxRecentReconnectFailCount = 5;
        int maxTotalReconnectFailCount = 30;

        long baseReconnectDelay = TimeUnit.SECONDS.toMillis(3);
        long deltaReconnectDelay = TimeUnit.SECONDS.toMillis(2);

        long responseTimeout = TimeUnit.MINUTES.toMillis(1);
        long responseDelay = TimeUnit.MILLISECONDS.toMillis(500);
        long verifyTimeout = TimeUnit.MINUTES.toMillis(10);

        int threadCount = 30;
        String password = "default-wrong-password";

        long idlePeriod = TimeUnit.SECONDS.toMillis(30);
    }
    Connection connection = new Connection();
}
