package cn.chuanwise.xiaoming.minecraft.xiaoming.configuration;

import cn.chuanwise.common.api.AbstractOriginalTagMarkable;
import cn.chuanwise.common.util.CollectionUtil;
import cn.chuanwise.common.util.Tags;
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
        return CollectionUtil.asSet(name, Tags.ALL);
    }
}
