package cn.chuanwise.xiaoming.minecraft.xiaoming.channel;

import cn.chuanwise.common.util.Preconditions;
import lombok.Data;

import java.util.*;

@Data
public class Channel {
    boolean enable = true;

    String name;

    Map<String, WorkGroup> workGroups = new HashMap<>();

    public boolean handle(Object object) {
        Preconditions.argumentNonNull(object);

        if (!enable) {
            return false;
        }
        boolean work = false;
        final Map<String, Object> environment = new HashMap<>();
        environment.put("channel", this);

        for (WorkGroup workGroup : workGroups.values()) {
            if (workGroup.work(object, environment)) {
                work = true;
            }
        }

        return work;
    }

    public Optional<WorkGroup> getWorkGroup(String name) {
        return Optional.ofNullable(workGroups.get(name));
    }
}