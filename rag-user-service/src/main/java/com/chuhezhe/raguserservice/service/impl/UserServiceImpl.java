package com.chuhezhe.raguserservice.service.impl;

import com.chuhezhe.common.constants.ErrorConstants;
import com.chuhezhe.common.entity.Result;
import com.chuhezhe.raguserservice.feign.UserServiceClient;
import com.chuhezhe.raguserservice.service.IUserService;
import com.chuhezhe.raguserservice.vo.request.LoginRequest;
import com.chuhezhe.raguserservice.vo.request.RegisterRequest;
import com.chuhezhe.raguserservice.vo.response.LoginResponse;
import com.chuhezhe.raguserservice.vo.response.RegisterResponse;
import feign.FeignException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements IUserService {

    private final UserServiceClient userServiceClient;

    @Override
    public Result<LoginResponse> login(LoginRequest loginRequest) {
        return userServiceClient.login(loginRequest);
    }

    @Override
    public Result<RegisterResponse> register(RegisterRequest registerRequest) {
        return userServiceClient.register(registerRequest);
    }
}
