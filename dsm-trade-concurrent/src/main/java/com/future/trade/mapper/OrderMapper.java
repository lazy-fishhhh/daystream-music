package com.future.trade.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.future.trade.entity.Order;
import com.future.trade.entity.vo.OrderDetailVO;
import org.mapstruct.Mapper;


@Mapper
public interface OrderMapper extends BaseMapper<Order> {
        IPage<OrderDetailVO> selectOrderDetailVOList(Page<OrderDetailVO> page, Long userId);
}
