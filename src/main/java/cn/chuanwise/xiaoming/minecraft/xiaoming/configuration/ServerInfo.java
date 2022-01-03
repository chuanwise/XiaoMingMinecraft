package cn.chuanwise.xiaoming.minecraft.xiaoming.configuration;

import cn.chuanwise.api.AbstractOriginalTagMarkable;
import cn.chuanwise.util.CollectionUtil;
import cn.chuanwise.util.TagUtil;
import lombok.Data;

import java.util.Set;

@Data
public class ServerInfo extends AbstractOriginalTagMarkable {
    String name;
    String password;

    long registerTimeMillis;
    long lastConnectTimeMillis;

    @Override
    public Set<String> getOriginalTags() {
        return CollectionUtil.asSet(name, TagUtil.ALL);
    }
}
