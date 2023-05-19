package cn.chuanwise.xiaoming.minecraft.bukkit.command;

import cn.chuanwise.commandlib.command.Command;
import cn.chuanwise.commandlib.event.*;
import cn.chuanwise.commandlib.handler.SharableHandlerAdapter;
import cn.chuanwise.mclib.bukkit.communicator.Communicator;
import cn.chuanwise.mclib.bukkit.net.Player;
import cn.chuanwise.pandalib.command.Properties;
import cn.chuanwise.common.util.CollectionUtil;
import cn.chuanwise.common.util.Throwables;
import cn.chuanwise.xiaoming.minecraft.bukkit.XMMCBukkitPlugin;
import org.bukkit.command.BlockCommandSender;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;

import java.util.*;
import java.util.stream.Collectors;

public class MessageHandler
        extends SharableHandlerAdapter {

    private static final Map<Class<?>, String> COMMAND_SENDER_NAMES;
    static {
        final Map<Class<?>, String> map = new HashMap<>();

        map.put(CommandSender.class, "通用");
        map.put(Player.class, "玩家");
        map.put(ConsoleCommandSender.class, "控制台");
        map.put(BlockCommandSender.class, "命令方块");

        COMMAND_SENDER_NAMES = Collections.unmodifiableMap(map);
    }

    @Override
    public boolean handleEvent(Object event) throws Exception {
        final XMMCBukkitPlugin plugin = XMMCBukkitPlugin.getInstance();
        final Communicator communicator = plugin.communicator();

        if (event instanceof CommandDispatchErrorEvent) {
            final CommandDispatchErrorEvent commandDispatchErrorEvent = (CommandDispatchErrorEvent) event;
            final CommandSender commandSender = (CommandSender) commandDispatchErrorEvent.getCommandSender();

            if (!(commandSender instanceof ConsoleCommandSender)) {
                communicator.error(commandSender, "command.error.dispatch");
            }
            communicator.consoleError("command.error.dispatch");
            communicator.consoleErrorString(Throwables.toStackTraces(commandDispatchErrorEvent.getCause()));
            return true;
        }

        if (event instanceof CommandExecuteErrorEvent) {
            final CommandExecuteErrorEvent commandExecuteErrorEvent = (CommandExecuteErrorEvent) event;
            final CommandSender commandSender = (CommandSender) commandExecuteErrorEvent.getCommandSender();

            if (!(commandSender instanceof ConsoleCommandSender)) {
                communicator.error(commandSender, "command.error.execute");
            }
            communicator.consoleError("command.error.execute");
            communicator.consoleErrorString(Throwables.toStackTraces(commandExecuteErrorEvent.getCause()));
            return true;
        }

        if (event instanceof MultipleCommandsMatchedEvent) {
            final MultipleCommandsMatchedEvent multipleCommandsMatchedEvent = (MultipleCommandsMatchedEvent) event;
            final CommandSender commandSender = (CommandSender) multipleCommandsMatchedEvent.getCommandSender();

            communicator.error(commandSender, "command.error.multiple");
            return true;
        }

        if (event instanceof FillFailedEvent) {
            final FillFailedEvent providerFailedEvent = (FillFailedEvent) event;
            final CommandSender commandSender = (CommandSender) providerFailedEvent.getCommandSender();

            communicator.error(commandSender, "command.failure.parse");
            return true;
        }

        if (event instanceof UnhandledCommandEvent) {
            final UnhandledCommandEvent unhandledCommandEvent = (UnhandledCommandEvent) event;
            final CommandSender commandSender = (CommandSender) unhandledCommandEvent.getCommandSender();

            final List<Command> commands = unhandledCommandEvent.getCommandTreeNode()
                    .getRelatedCommands()
                    .stream()
                    .filter(x -> {
                        return x.getProperty(Properties.PERMISSION)
                                .map(commandSender::hasPermission)
                                .orElse(true);
                    })
                    .sorted(Comparator.comparing(Command::getFormat))
                    .collect(Collectors.toList());

            if (commands.isEmpty()) {
                communicator.error(commandSender, "command.error.unhandled.empty");
            } else if (commands.size() == 1) {
                communicator.error(commandSender, "command.error.unhandled.singleton", commands.get(0).getFormat());
            } else {
                communicator.warn(commandSender, "command.error.unhandled.list", CollectionUtil.toString(commands,
                        c -> "§8> §f/" + c.getFormat() + "§r", "\n"));
            }
            return true;
        }

        if (event instanceof MismatchedFormatEvent) {
            final MismatchedFormatEvent mismatchedFormatEvent = (MismatchedFormatEvent) event;
            final CommandSender commandSender = (CommandSender) mismatchedFormatEvent.getCommandSender();

            communicator.error(commandSender, "command.error.format");
            return true;
        }

        return false;
    }
}
