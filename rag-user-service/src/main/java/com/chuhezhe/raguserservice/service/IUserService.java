package com.chuhezhe.raguserservice.service;

import com.chuhezhe.common.entity.Result;
import com.chuhezhe.raguserservice.vo.request.LoginRequest;
import com.chuhezhe.raguserservice.vo.request.RegisterRequest;
import com.chuhezhe.raguserservice.vo.response.LoginResponse;
import com.chuhezhe.raguserservice.vo.response.RegisterResponse;

public interface IUserService {

    Result<LoginResponse> login(LoginRequest loginRequest);


    Result<RegisterResponse> register(RegisterRequest registerRequest);
}
