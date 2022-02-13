package cn.chuanwise.xiaoming.minecraft.bukkit.command;

import cn.chuanwise.commandlib.command.Command;
import cn.chuanwise.commandlib.event.*;
import cn.chuanwise.commandlib.handler.CommandLibHandlerAdapter;
import cn.chuanwise.mclib.bukkit.communicator.Communicator;
import cn.chuanwise.mclib.bukkit.net.Player;
import cn.chuanwise.util.CollectionUtil;
import cn.chuanwise.util.Strings;
import cn.chuanwise.util.Throwables;
import cn.chuanwise.xiaoming.minecraft.bukkit.XMMCBukkitPlugin;
import org.bukkit.command.BlockCommandSender;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;

import java.util.*;
import java.util.stream.Collectors;

public class MessageHandler
        extends CommandLibHandlerAdapter {

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
            final CommandSender commandSender = (CommandSender) commandDispatchErrorEvent
                    .getDispatchContext().getCommandSender();

            if (!(commandSender instanceof ConsoleCommandSender)) {
                communicator.error(commandSender, "command.error.dispatch");
            }
            communicator.consoleError("command.error.dispatch");
            communicator.consoleErrorString(Throwables.toStackTraces(commandDispatchErrorEvent.getCause()));
            return true;
        }

        if (event instanceof CommandExecuteErrorEvent) {
            final CommandExecuteErrorEvent commandExecuteErrorEvent = (CommandExecuteErrorEvent) event;
            final CommandSender commandSender = (CommandSender) commandExecuteErrorEvent.getCommandContext().getCommandSender();

            if (!(commandSender instanceof ConsoleCommandSender)) {
                communicator.error(commandSender, "command.error.execute");
            }
            communicator.consoleError("command.error.execute");
            communicator.consoleErrorString(Throwables.toStackTraces(commandExecuteErrorEvent.getCause()));
            return true;
        }

        if (event instanceof MultipleCommandsMatchedEvent) {
            final MultipleCommandsMatchedEvent multipleCommandsMatchedEvent = (MultipleCommandsMatchedEvent) event;
            final CommandSender commandSender = (CommandSender) multipleCommandsMatchedEvent.getDispatchContext().getCommandSender();

            communicator.error(commandSender, "command.error.multiple");
            return true;
        }

        if (event instanceof ParseErrorEvent) {
            final ParseErrorEvent parseErrorEvent = (ParseErrorEvent) event;
            final CommandSender commandSender = (CommandSender) parseErrorEvent.getCommandContext().getCommandSender();

            final Class<?> requiredClass = parseErrorEvent.getRequiredClass();
            if (CommandSender.class.isAssignableFrom(requiredClass)) {
                final String name = Optional.ofNullable(COMMAND_SENDER_NAMES.get(requiredClass))
                        .orElse(" " + requiredClass.getSimpleName() + " ");
                communicator.error(commandSender, "command.error.commandSender", name);
            } else {
                communicator.error(commandSender, "command.error.parse");
            }
            return true;
        }

        if (event instanceof ParseFailedEvent) {
            final ParseFailedEvent parseFailedEvent = (ParseFailedEvent) event;
            final CommandSender commandSender = (CommandSender) parseFailedEvent.getCommandContext().getCommandSender();

            communicator.error(commandSender, "command.failure.parse");
            return true;
        }

        if (event instanceof PermissionDeniedEvent) {
            final PermissionDeniedEvent permissionDeniedEvent = (PermissionDeniedEvent) event;
            final CommandSender commandSender = (CommandSender) permissionDeniedEvent.getCommandContext().getCommandSender();

            communicator.error(commandSender, "command.permission", permissionDeniedEvent.getPermission());
            return true;
        }

        if (event instanceof UnhandledCommandEvent) {
            final UnhandledCommandEvent unhandledCommandEvent = (UnhandledCommandEvent) event;
            final CommandSender commandSender = (CommandSender) unhandledCommandEvent.getDispatchContext().getCommandSender();

            final List<Command> commands = unhandledCommandEvent.getCommandTree()
                    .getSubCommands()
                    .stream()
                    .filter(x -> {
                        final String permission = x.getPermission();
                        return Strings.isEmpty(permission) || commandSender.hasPermission(permission);
                    })
                    .sorted(Comparator.comparing(Command::getUsage))
                    .collect(Collectors.toList());

            if (commands.isEmpty()) {
                communicator.error(commandSender, "command.error.unhandled.empty");
            } else {
                communicator.warn(commandSender, "command.error.unhandled.list", CollectionUtil.toString(commands,
                        c -> "§7> §f" + c.getUsage()));
            }
            return true;
        }

        if (event instanceof WrongFormatEvent) {
            final WrongFormatEvent wrongFormatEvent = (WrongFormatEvent) event;
            final CommandSender commandSender = (CommandSender) wrongFormatEvent.getDispatchContext().getCommandSender();

            communicator.error(commandSender, "command.error.format");
            return true;
        }

        return false;
    }
}
