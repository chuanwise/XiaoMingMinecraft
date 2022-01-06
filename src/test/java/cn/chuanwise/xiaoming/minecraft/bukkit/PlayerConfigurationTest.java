package cn.chuanwise.xiaoming.minecraft.bukkit;

import cn.chuanwise.xiaoming.minecraft.xiaoming.configuration.PlayerConfiguration;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class PlayerConfigurationTest {
    private PlayerConfiguration playerConfiguration;

    @BeforeAll
    void init() {
        playerConfiguration = new PlayerConfiguration();
    }

    @Test
    void testBind() {
        Assertions.assertEquals(PlayerConfiguration.BindReceipt.SUCCEED, playerConfiguration.forceBind(123, "123"));
        Assertions.assertEquals(PlayerConfiguration.BindReceipt.REPEAT, playerConfiguration.forceBind(123, "123"));
        Assertions.assertEquals(PlayerConfiguration.BindReceipt.OTHER, playerConfiguration.forceBind(456, "123"));
    }

    @Test
    void testUnbind() {
        playerConfiguration.forceBind(123, "123");
        Assertions.assertTrue(playerConfiguration.unbind(123, "123"));
        Assertions.assertFalse(playerConfiguration.unbind(123, "123"));
    }

    @Test
    void testAll() {

    }
}
