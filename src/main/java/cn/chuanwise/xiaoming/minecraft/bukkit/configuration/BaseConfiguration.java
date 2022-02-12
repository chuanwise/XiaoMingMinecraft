package cn.chuanwise.xiaoming.minecraft.bukkit.configuration;

import cn.chuanwise.storage.file.StoredFile;
import lombok.Data;

@Data
public class BaseConfiguration extends StoredFile {
    boolean debug;
}
