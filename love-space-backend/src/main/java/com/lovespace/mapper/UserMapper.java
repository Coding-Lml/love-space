package com.lovespace.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lovespace.entity.User;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface UserMapper extends BaseMapper<User> {
}
