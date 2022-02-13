package cn.chuanwise.xiaoming.minecraft.bukkit;

import cn.chuanwise.mclib.bukkit.communicator.Communicator;
import cn.chuanwise.mclib.storage.Language;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class CommunicatorTest {
    private Communicator communicator;
    private Language language;

    @BeforeAll
    void init() {
        language = new Language();
        communicator = new Communicator(null, language);
    }

    @Test
    void testNull() {
        System.out.println(language.formatInfoNode("qwq"));
    }
}
