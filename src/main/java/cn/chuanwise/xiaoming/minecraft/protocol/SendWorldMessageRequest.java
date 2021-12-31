package cn.chuanwise.xiaoming.minecraft.protocol;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.HashSet;
import java.util.Set;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class SendWorldMessageRequest {
    Set<String> worldNames = new HashSet<>();
    Set<String> messages = new HashSet<>();
}
