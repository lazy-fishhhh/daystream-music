package message;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class ExchangeVipMessage {
    private Long userId;
    private Long taskId;

}
