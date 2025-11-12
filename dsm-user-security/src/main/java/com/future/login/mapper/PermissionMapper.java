package com.future.login.mapper;


import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import entity.pojo.info.Permission;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface PermissionMapper extends BaseMapper<Permission> {
    List<String> selectPermissionByUserId(Long userId);
}
