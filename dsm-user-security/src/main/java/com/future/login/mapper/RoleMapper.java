package com.future.login.mapper;


import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import entity.pojo.info.Role;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;
@Mapper
public interface RoleMapper extends BaseMapper<Role> {
    List<String> selectRoleByUserId(Long userId);
}
