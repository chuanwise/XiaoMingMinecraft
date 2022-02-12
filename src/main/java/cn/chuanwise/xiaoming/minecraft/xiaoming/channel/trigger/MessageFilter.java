package cn.chuanwise.xiaoming.minecraft.xiaoming.channel.trigger;

import cn.chuanwise.pattern.ParameterPattern;
import cn.chuanwise.xiaoming.minecraft.xiaoming.XMMCXiaoMingPlugin;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.beans.Transient;
import java.util.*;
import java.util.regex.Matcher;

@SuppressWarnings("all")
public abstract class MessageFilter {
    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class StartWith extends MessageFilter {
        String head;

        @Override
        public String getDescription() {
            return "以“" + head + "”开头";
        }

        @Override
        public MessageFilterReceipt filter0(String message) {
            return message.startsWith(head) && message.length() > head.length()
                    ? new MessageFilterReceipt.Accepted(Collections.singletonMap("head", head), message.substring(head.length()))
                    : MessageFilterReceipt.Denied.getInstance();
        }
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class EndWith extends MessageFilter {
        String tail;

        @Override
        public String getDescription() {
            return "以“" + tail + "”结尾";
        }

        @Override
        public MessageFilterReceipt filter0(String message) {
            return message.endsWith(tail) && message.length() > tail.length()
                    ? new MessageFilterReceipt.Accepted(Collections.singletonMap("tail", tail), message.substring(0, message.length() - tail.length()))
                    : MessageFilterReceipt.Denied.getInstance();
        }
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class ContainsEqual extends MessageFilter {
        String text;

        @Override
        public String getDescription() {
            return "包含“" + text + "”";
        }

        @Override
        public MessageFilterReceipt filter0(String message) {
            return message.contains(text)
                    ? new MessageFilterReceipt.Accepted(Collections.singletonMap("text", text), message)
                    : MessageFilterReceipt.Denied.getInstance();
        }
    }

    @NoArgsConstructor
    public static abstract class Pattern extends MessageFilter {
        @Getter
        String regex;
        transient java.util.regex.Pattern pattern;

        public Pattern(String regex) {
            setRegex(regex);
        }

        @Transient
        public java.util.regex.Pattern getPattern() {
            if (Objects.isNull(pattern)) {
                pattern = java.util.regex.Pattern.compile(regex);
            }
            return pattern;
        }

        public void setRegex(String regex) {
            this.regex = regex;
            pattern = java.util.regex.Pattern.compile(regex);
        }
    }

    @NoArgsConstructor
    public static class Match extends Pattern {
        public Match(String regex) {
            super(regex);
        }

        @Override
        public String getDescription() {
            return "匹配：" + regex;
        }

        @Override
        public MessageFilterReceipt filter0(String message) {
            final Matcher matcher = getPattern().matcher(message);
            if (!matcher.matches()) {
                return MessageFilterReceipt.Denied.getInstance();
            }

            final String group = matcher.group();
            final Map<String, String> environment = Collections.singletonMap("match", group);

            return new MessageFilterReceipt.Accepted((Map) environment, message);
        }
    }

    @NoArgsConstructor
    public static class StartMatch extends Pattern {
        public StartMatch(String regex) {
            super(regex);
        }

        @Override
        public String getDescription() {
            return "开头匹配：" + regex;
        }

        @Override
        public MessageFilterReceipt filter0(String message) {
            final Matcher matcher = getPattern().matcher(message);
            if (!matcher.find() || matcher.start() != 0) {
                return MessageFilterReceipt.Denied.getInstance();
            }

            final String group = matcher.group();
            final Map<String, String> environment = Collections.singletonMap("match", group);

            return new MessageFilterReceipt.Accepted((Map) environment, message);
        }
    }

    @NoArgsConstructor
    public static class EndMatch extends Pattern {
        public EndMatch(String regex) {
            super(regex);
        }

        @Override
        public String getDescription() {
            return "结尾匹配：" + regex;
        }

        @Override
        public MessageFilterReceipt filter0(String message) {
            final Matcher matcher = getPattern().matcher(message);
            if (!matcher.find() || matcher.end() != message.length()) {
                return MessageFilterReceipt.Denied.getInstance();
            }

            final String group = matcher.group();
            final Map<String, String> environment = Collections.singletonMap("match", group);

            return new MessageFilterReceipt.Accepted((Map) environment, message);
        }
    }

    @NoArgsConstructor
    public static class ContainMatch extends Pattern {
        public ContainMatch(String regex) {
            super(regex);
        }

        @Override
        public String getDescription() {
            return "包含匹配：" + regex;
        }

        @Override
        public MessageFilterReceipt filter0(String message) {
            final Matcher matcher = getPattern().matcher(message);
            if (!matcher.find()) {
                return MessageFilterReceipt.Denied.getInstance();
            }

            final String group = matcher.group();
            final Map<String, String> environment = Collections.singletonMap("match", group);

            return new MessageFilterReceipt.Accepted((Map) environment, message);
        }
    }

    @NoArgsConstructor
    public static class Format extends MessageFilter {
        String format;
        transient ParameterPattern pattern;

        public Format(String format) {
            setFormat(format);
        }

        public void setFormat(String format) {
            this.format = format;
        }

        @Transient
        public ParameterPattern getPattern() {
            if (Objects.isNull(pattern)) {
                pattern = new ParameterPattern(format);
            }
            return pattern;
        }

        @Override
        public String getDescription() {
            return "匹配格式：" + format;
        }

        @Override
        public MessageFilterReceipt filter0(String message) {
            final int maxIterateTime = XMMCXiaoMingPlugin.getInstance().getXiaoMingBot().getConfiguration().getMaxIterateTime();
            final Optional<Map<String, String>> optionalEnvironment = getPattern().parse(message);

            if (!optionalEnvironment.isPresent()) {
                return MessageFilterReceipt.Denied.getInstance();
            } else {
                final Map<String, String> environment = optionalEnvironment.get();
                return new MessageFilterReceipt.Accepted((Map) environment, message);
            }
        }
    }

    public static class All extends MessageFilter {
        @Override
        public String getDescription() {
            return "所有消息";
        }

        @Override
        public MessageFilterReceipt filter0(String message) {
            return new MessageFilterReceipt.Accepted(Collections.emptyMap(), message);
        }
    }

    public abstract String getDescription();

    protected abstract MessageFilterReceipt filter0(String message);

    public final MessageFilterReceipt filter(String message) {
        final MessageFilterReceipt receipt = filter0(message);
        if (receipt instanceof MessageFilterReceipt.Accepted) {
            final MessageFilterReceipt.Accepted accepted = (MessageFilterReceipt.Accepted) receipt;

            final Map<String, Object> environment = new HashMap<>();
            environment.put("message", accepted.getMessage());
            environment.putAll(accepted.getEnvironment());

            return new MessageFilterReceipt.Accepted(environment, accepted.getMessage());
        } else {
            return receipt;
        }
    }
}
