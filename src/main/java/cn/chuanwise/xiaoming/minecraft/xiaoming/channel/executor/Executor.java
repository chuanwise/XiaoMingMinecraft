package cn.chuanwise.xiaoming.minecraft.xiaoming.channel.executor;

import cn.chuanwise.xiaoming.minecraft.xiaoming.channel.executor.server.ServerBroadcastExecutor;
import cn.chuanwise.xiaoming.minecraft.xiaoming.channel.executor.server.ServerConsoleCommandExecutor;
import cn.chuanwise.xiaoming.minecraft.xiaoming.channel.executor.xiaoming.GroupBroadcastExecutor;
import cn.chuanwise.xiaoming.minecraft.xiaoming.channel.executor.xiaoming.PrivateBroadcastExecutor;
import cn.chuanwise.xiaoming.minecraft.xiaoming.channel.executor.xiaoming.XiaoMingSendMessageExecutor;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public abstract class Executor {
    /** 执行器类型名 */
    public static final Map<Class<?>, String> EXECUTOR_NAMES;
    static {
        final Map<Class<?>, String> names = new HashMap<>();

        names.put(ServerConsoleCommandExecutor.class, "服务器控制台命令执行器");
        names.put(ServerBroadcastExecutor.class, "服务器广播执行器");

        names.put(GroupBroadcastExecutor.class, "群聊广播执行器");
        names.put(PrivateBroadcastExecutor.class, "私聊广播执行器");
        names.put(XiaoMingSendMessageExecutor.class, "回复执行器");

        EXECUTOR_NAMES = Collections.unmodifiableMap(names);
    }

    /**
     * 执行，并扩展环境
     * @param environment 执行环境表
     */
    public abstract void execute(Map<String, Object> environment);
}
