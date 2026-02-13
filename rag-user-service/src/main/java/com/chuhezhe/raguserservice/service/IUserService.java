package com.chuhezhe.raguserservice.service;

import com.chuhezhe.common.entity.Result;
import com.chuhezhe.raguserservice.dto.UserLoginDTO;
import com.chuhezhe.raguserservice.dto.UserRegisterDTO;
import com.chuhezhe.raguserservice.vo.UserLoginVo;
import com.chuhezhe.raguserservice.vo.UserRegisterVO;

public interface IUserService {

    Result<UserLoginVo> login(UserLoginDTO loginRequest);


    Result<UserRegisterVO> register(UserRegisterDTO registerRequest);
}
