package cn.chuanwise.xiaoming.minecraft.bukkit;

import cn.chuanwise.mclib.storage.Language;
import cn.chuanwise.storage.file.StoredFile;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.NoSuchElementException;

public class LanguageTest {
    @Test
    void testLoadLanguage() throws IOException {
        final Language language = StoredFile.loadYamlResource("language.yml", Language.class);
        System.out.println(language.getNakedMessage("net.verify.error.exception").orElseThrow(NoSuchElementException::new));
    }
}
