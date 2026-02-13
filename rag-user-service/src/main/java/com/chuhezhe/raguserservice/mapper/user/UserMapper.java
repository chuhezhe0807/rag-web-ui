package com.chuhezhe.raguserservice.mapper.user;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.chuhezhe.raguserservice.entity.User;

public interface UserMapper extends BaseMapper<User> {
    /**
     * 根据用户名查询用户
     * @param username 用户名
     * @return
     */
    User getUserByUsername(String username);

    /**
     * 根据邮箱查询用户
     * @param email 邮箱
     * @return
     */
    User getUserByEmail(String email);
}
