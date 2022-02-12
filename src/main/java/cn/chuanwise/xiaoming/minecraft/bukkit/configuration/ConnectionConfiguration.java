package cn.chuanwise.xiaoming.minecraft.bukkit.configuration;

import cn.chuanwise.storage.file.StoredFile;
import lombok.Data;

import java.util.concurrent.TimeUnit;

@Data
public class ConnectionConfiguration extends StoredFile {
    boolean autoConnect = true;

    String host = "127.0.0.1";

    int port = 23333;

    int maxAttemptCount = 10;

    long baseReconnectDelay = TimeUnit.SECONDS.toMillis(3);
    long deltaReconnectDelay = TimeUnit.SECONDS.toMillis(2);

    long responseTimeout = TimeUnit.SECONDS.toMillis(30);
    long responseDelay = TimeUnit.MILLISECONDS.toMillis(500);
    long verifyTimeout = TimeUnit.MINUTES.toMillis(10);

    int threadCount = 30;
    String password = "default-wrong-password";

    long heartbeatTimeout = TimeUnit.SECONDS.toMillis(30);
}
