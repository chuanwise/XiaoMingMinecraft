package cn.chuanwise.xiaoming.minecraft.protocol;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class SendWorldMessageRequest {
    Set<String> worldNames = new HashSet<>();
    List<String> messages = new ArrayList<>();
}
