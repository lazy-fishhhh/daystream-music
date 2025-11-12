package com.future.login.mapper;


import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import entity.pojo.info.User;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface UserMapper extends BaseMapper<User> {
}
