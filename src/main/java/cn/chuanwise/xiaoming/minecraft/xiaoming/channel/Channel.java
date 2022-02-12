package cn.chuanwise.xiaoming.minecraft.xiaoming.channel;

import cn.chuanwise.util.Preconditions;
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
        for (WorkGroup workGroup : workGroups.values()) {
            if (workGroup.work(object)) {
                work = true;
            }
        }

        return work;
    }

    public Optional<WorkGroup> getWorkGroup(String name) {
        return Optional.ofNullable(workGroups.get(name));
    }
}