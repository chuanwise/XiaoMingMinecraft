package cn.chuanwise.xiaoming.minecraft.xiaoming.configuration;

import cn.chuanwise.api.AbstractOriginalTagMarkable;
import cn.chuanwise.util.CollectionUtil;
import lombok.Data;

import java.util.Set;

@Data
public class ServerInfo extends AbstractOriginalTagMarkable {
    String name;
    String password;

    @Override
    public Set<String> getOriginalTags() {
        return CollectionUtil.asSet(name);
    }
}
