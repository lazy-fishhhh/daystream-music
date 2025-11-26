package com.future.trade.controller; 
 
 import jakarta.validation.constraints.Max; 
 import jakarta.validation.constraints.Min; 
 import com.future.trade.constants.BasicOrderConstants; 
 import com.future.trade.service.IBasicOrderService; 
 import entity.pojo.util.Result; 
 import io.swagger.annotations.ApiOperation; 
 import lombok.RequiredArgsConstructor; 
 import org.springframework.validation.annotation.Validated; 
 import org.springframework.web.bind.annotation.*; 
 
 import jakarta.validation.constraints.NotNull;

@RestController
@RequiredArgsConstructor
@RequestMapping("/basicOrder")
@Validated
public class BasicOrderController {

    private final IBasicOrderService basicOrderService;

    @ApiOperation("音乐人打赏")
    @GetMapping("/reward")
    public Result<String> rewardBasicOrder(@NotNull(message = "接收人ID不能为空")
                                           @RequestParam Long receiverId,
                                           @RequestParam
                                           @Min(value = BasicOrderConstants.MIN_REWARD_AMOUNT, message = "打赏金额必须大于等于" + BasicOrderConstants.MIN_REWARD_AMOUNT)
                                           @Max(value = BasicOrderConstants.MAX_REWARD_AMOUNT, message = "打赏金额必须小于等于" + BasicOrderConstants.MAX_REWARD_AMOUNT)
                                           Integer amount) {
        return basicOrderService.rewardBasicOrder(receiverId, amount);
    }
}
