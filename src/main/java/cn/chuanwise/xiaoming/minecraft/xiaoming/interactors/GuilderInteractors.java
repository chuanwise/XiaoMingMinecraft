package cn.chuanwise.xiaoming.minecraft.xiaoming.interactors;

import cn.chuanwise.util.CollectionUtil;
import cn.chuanwise.util.Collections;
import cn.chuanwise.util.NumberUtil;
import cn.chuanwise.xiaoming.annotation.Filter;
import cn.chuanwise.xiaoming.annotation.Required;
import cn.chuanwise.xiaoming.contact.contact.GroupContact;
import cn.chuanwise.xiaoming.interactor.SimpleInteractors;
import cn.chuanwise.xiaoming.minecraft.xiaoming.XMMCXiaoMingPlugin;
import cn.chuanwise.xiaoming.minecraft.xiaoming.channel.Channel;
import cn.chuanwise.xiaoming.minecraft.xiaoming.channel.WorkGroup;
import cn.chuanwise.xiaoming.minecraft.xiaoming.channel.executor.server.ServerBroadcastExecutor;
import cn.chuanwise.xiaoming.minecraft.xiaoming.channel.executor.xiaoming.GroupBroadcastExecutor;
import cn.chuanwise.xiaoming.minecraft.xiaoming.channel.trigger.MessageFilter;
import cn.chuanwise.xiaoming.minecraft.xiaoming.channel.trigger.server.PlayerChatTrigger;
import cn.chuanwise.xiaoming.minecraft.xiaoming.channel.trigger.xiaoming.GroupMessageTrigger;
import cn.chuanwise.xiaoming.minecraft.xiaoming.configuration.ChannelConfiguration;
import cn.chuanwise.xiaoming.minecraft.xiaoming.configuration.SessionConfiguration;
import cn.chuanwise.xiaoming.minecraft.xiaoming.configuration.ServerInfo;
import cn.chuanwise.xiaoming.minecraft.xiaoming.util.Words;
import cn.chuanwise.xiaoming.user.GroupXiaoMingUser;
import cn.chuanwise.xiaoming.user.XiaoMingUser;
import cn.chuanwise.xiaoming.util.MiraiCodes;

import java.util.*;

public class GuilderInteractors extends SimpleInteractors<XMMCXiaoMingPlugin> {
    @Filter(Words.TWO_WAY + Words.CHAT + Words.CHANNEL + Words.GUILDER)
    @Required("xmmc.admin.channel.add")
    void addChannelGuilder(XiaoMingUser user) {
        final SessionConfiguration configuration = plugin.getSessionConfiguration();
        final Map<String, ServerInfo> servers = configuration.getServers();

        if (servers.isEmpty()) {
            user.sendWarning("小明还不认识任何服务器，在使用「迎接新服务器」添加服务器后再使用互通向导吧");
            return;
        }

        // choose server info
        final ServerInfo serverInfo;
        if (servers.size() == 1) {
            serverInfo = servers.values().iterator().next();
        } else {
            user.sendMessage("你不只有一个服务器：\n" +
                    CollectionUtil.toIndexString(servers.values(), ServerInfo::getName) + "\n" +
                    "告诉小明你希望创建互通频道的服务器名吧！");
            ServerInfo tempServerInfo = null;
            while (true) {
                final String serverName = user.nextMessageOrExit().serialize();
                tempServerInfo = servers.get(serverName);

                if (Objects.isNull(tempServerInfo)) {
                    user.sendMessage("找不到服务器「" + serverName + "」，再输入一遍服务器名吧！");
                } else {
                    break;
                }
            }
            serverInfo = tempServerInfo;
        }

        // choose group
        long groupCode;

        if (user instanceof GroupXiaoMingUser) {
            groupCode = ((GroupXiaoMingUser) user).getGroupCode();
        } else {
            user.sendMessage("需要和服务器「" + serverInfo.getName() + "」互通的 QQ 群群号是？");
            while (true) {
                final String groupCodeString = user.nextMessageOrExit().serialize();
                final Optional<Long> optionalGroupCode = NumberUtil.parseLong(groupCodeString);

                if (optionalGroupCode.isPresent()) {
                    groupCode = optionalGroupCode.get();
                    break;
                } else {
                    user.sendError("「" + groupCodeString + "」好像并不是合理的群号，重新输入一遍吧");
                }
            }

            // get group contact
            final Optional<GroupContact> optionalGroupContact = xiaoMingBot.getContactManager().getGroupContact(groupCode);
            if (!optionalGroupContact.isPresent()) {
                user.sendWarning("小明好像还不在这个群里，你确定要用这个群作为互通群吗？如果是，请回复我「是」，其他任何回复将取消操作");
                if (!Objects.equals(user.nextMessageOrExit().serialize(), "是")) {
                    user.sendMessage("操作已取消");
                    return;
                }
            }
        }

        final String groupTag = String.valueOf(groupCode);
        final ChannelConfiguration channelConfiguration = plugin.getChannelConfiguration();

        // msg filter
        final MessageFilter messageFilter;
        user.sendMessage("你希望转发所有消息还是以确定开头的消息呢？\n" +
                "如果转发所有消息，回复「所有」，或回复一个消息开头（建议尽可能短，例如 #）");
        final String messageHead = user.nextMessageOrExit().serialize();
        if (Objects.equals(messageHead, "所有")) {
            messageFilter = new MessageFilter.All();
        } else {
            messageFilter = new MessageFilter.StartWith(MiraiCodes.contentToString(messageHead));
        }

        // config player => group
        final String playerChatWorkGroupName = "玩家聊天";
        final WorkGroup playerChatWorkGroup = new WorkGroup();
        playerChatWorkGroup.setName(playerChatWorkGroupName);

        final PlayerChatTrigger playerChatTrigger = new PlayerChatTrigger();
        playerChatTrigger.setMessageFilter(messageFilter);

        final GroupBroadcastExecutor playerChatExecutor = new GroupBroadcastExecutor();
        playerChatExecutor.setGroupTag(groupTag);
        playerChatExecutor.setFormat("{playerOrAlias}：{message}");

        playerChatWorkGroup.setTrigger(playerChatTrigger);
        playerChatWorkGroup.setExecutors(Collections.asList(playerChatExecutor));

        // config group => server
        final String groupMessageWorkGroupName = "群聊消息";
        final WorkGroup groupMessageWorkGroup = new WorkGroup();
        groupMessageWorkGroup.setName(groupMessageWorkGroupName);

        final GroupMessageTrigger groupMessageTrigger = new GroupMessageTrigger();
        groupMessageTrigger.setMessageFilter(messageFilter);
        groupMessageTrigger.setGroupTag(groupTag);

        final ServerBroadcastExecutor groupMessageExecutor = new ServerBroadcastExecutor();
        groupMessageExecutor.setFormat("§7[§3{contact.alias}§7] §b{sender.alias} §8§l: §r{message}");

        groupMessageWorkGroup.setTrigger(groupMessageTrigger);
        groupMessageWorkGroup.setExecutors(Collections.asList(groupMessageExecutor));

        // add to channel
        final String channelName;
        if (Objects.isNull(channelConfiguration.getChannels().get(serverInfo.getName()))) {
            channelName = serverInfo.getName();
        } else {
            user.sendMessage("给频道起一个名字吧！");
            while (true) {
                final String tempChannelName = user.nextMessageOrExit().serialize();
                if (Objects.isNull(channelConfiguration.getChannels().get(serverInfo.getName()))) {
                    channelName = tempChannelName;
                    break;
                } else {
                    user.sendError("已存在频道「" + tempChannelName + "」，重新指定一个名字吧");
                }
            }
        }

        // channel
        final Channel channel = new Channel();
        channel.setName(channelName);

        channel.getWorkGroups().put(playerChatWorkGroupName, playerChatWorkGroup);
        channel.getWorkGroups().put(groupMessageWorkGroupName, groupMessageWorkGroup);

        channelConfiguration.getChannels().put(channelName, channel);
        channelConfiguration.readyToSave();
        user.sendMessage("成功建立服务器「" + serverInfo.getName() + "」" +
                "和群聊「" + xiaoMingBot.getGroupInformationManager().getAliasAndCode(groupCode) + "」" +
                "之间的双向互通频道「" + channelName + "」，转发规则：" + messageFilter.getDescription());
    }
}
