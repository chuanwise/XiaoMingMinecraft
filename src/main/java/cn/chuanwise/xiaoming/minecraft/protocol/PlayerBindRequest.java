package cn.chuanwise.xiaoming.minecraft.protocol;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class PlayerBindRequest {
    String playerName;
    long accountCode;
}
