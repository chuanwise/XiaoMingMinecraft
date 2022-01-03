package cn.chuanwise.xiaoming.minecraft.protocol;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class AskRequest {
    String playerName;
    String message;
    long timeout;
}
