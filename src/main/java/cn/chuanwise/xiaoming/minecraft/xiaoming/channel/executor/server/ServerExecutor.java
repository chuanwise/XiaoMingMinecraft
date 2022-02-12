package cn.chuanwise.xiaoming.minecraft.xiaoming.channel.executor.server;

import cn.chuanwise.xiaoming.minecraft.xiaoming.channel.executor.Executor;
import cn.chuanwise.xiaoming.minecraft.xiaoming.channel.executor.ServerTagExecutor;
import lombok.Data;

import java.util.Map;

@Data
public abstract class ServerExecutor
        extends Executor
        implements ServerTagExecutor {
    String serverTag;
}
