package cn.chuanwise.xiaoming.minecraft.xiaoming.command;

import cn.chuanwise.common.pattern.ParameterPattern;
import cn.chuanwise.common.util.Tags;
import cn.chuanwise.xiaoming.permission.Permission;
import lombok.Data;

import java.beans.Transient;
import java.util.*;

@Data
public class Command {
    /** 自定义指令名 */
    String name;

    /** 自定义指令格式 */
    Set<String> formats = new HashSet<>();
    transient Set<ParameterPattern> compiledFormats = new HashSet<>();
    @Transient
    public Set<ParameterPattern> getCompiledFormats() {
        if (compiledFormats.size() == formats.size()) {
            return compiledFormats;
        } else {
            setFormats(formats);
            return compiledFormats;
        }
    }

    /** 指令所需的权限 */
    List<String> permissions = new ArrayList<>();
    transient Set<Permission> compiledPermissions = new HashSet<>();
    @Transient
    public Set<Permission> getCompiledPermissions() {
        if (compiledPermissions.size() == permissions.size()) {
            return compiledPermissions;
        } else {
            setPermissions(permissions);
            return compiledPermissions;
        }
    }

    /** 需要在什么服务器上执行指令 */
    String serverTag;

    /** 需要在服务器上执行的指令 */
    List<String> serverCommands = new ArrayList<>();

    /** 执行指令的身份 */
    CommandIdentify identify = CommandIdentify.CONSOLE;

    /** 执行指令所需的用户 tag */
    String accountTag = Tags.ALL;

    /**
     * 如果为 {@code null}，则指令可以在群或私聊执行
     * 否则必须在带有该标签的群内执行
     */
    String groupTag;

    /** 执行完后是否不向后搜索 */
    boolean nonNext = true;

    /** 是否绑定用户名才能执行 */
    boolean mustBind = false;

    /** 是否允许不完整的执行过程 */
    boolean allowUncompletedExecution = false;

    public void setFormats(Set<String> formats) {
        this.formats = formats;
        compiledFormats.clear();
        formats.stream()
                .map(ParameterPattern::new)
                .forEach(compiledFormats::add);
    }

    public void setPermissions(List<String> permissions) {
        this.permissions = permissions;
        compiledPermissions.clear();
        permissions.stream()
                .map(Permission::compile)
                .forEach(compiledPermissions::add);
    }
}
