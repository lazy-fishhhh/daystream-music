package com.future.trade.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.future.trade.entity.Order;
import org.mapstruct.Mapper;


@Mapper
public interface OrderMapper extends BaseMapper<Order> {
}
