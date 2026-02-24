package com.chuhezhe.raguserservice.service;

import com.chuhezhe.common.entity.Result;
import com.chuhezhe.raguserservice.dto.UserLoginDTO;
import com.chuhezhe.raguserservice.dto.UserRegisterDTO;
import com.chuhezhe.raguserservice.vo.UserLoginVO;
import com.chuhezhe.raguserservice.vo.UserRegisterVO;
import com.chuhezhe.raguserservice.vo.UserVO;

public interface IUserService {

    Result<UserLoginVO> login(UserLoginDTO loginRequest);


    Result<UserRegisterVO> register(UserRegisterDTO registerRequest);

    Result<UserVO> getUserInfo(String token);
}
