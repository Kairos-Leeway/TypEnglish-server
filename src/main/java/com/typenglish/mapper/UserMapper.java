package com.typenglish.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.typenglish.entity.User;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface UserMapper extends BaseMapper<User> {
}
