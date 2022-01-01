package cn.chuanwise.xiaoming.minecraft.bukkit;

import cn.chuanwise.xiaoming.minecraft.bukkit.net.Client;
import org.junit.Test;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.TestInstance;

import java.util.concurrent.ExecutionException;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class ClientTest {

    private Client client;

    @BeforeAll
    void init() {
        client = new Client(new Plugin());
    }

    @Test
    void testConnect() throws ExecutionException, InterruptedException {
        client.connect().orElseThrow().get();
    }
}
