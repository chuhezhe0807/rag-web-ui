package com.chuhezhe.raguserservice.controller;

import com.chuhezhe.common.entity.Result;
import com.chuhezhe.raguserservice.service.IUserService;
import com.chuhezhe.raguserservice.vo.request.LoginRequest;
import com.chuhezhe.raguserservice.vo.request.RegisterRequest;
import com.chuhezhe.raguserservice.vo.response.LoginResponse;
import com.chuhezhe.raguserservice.vo.response.RegisterResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/ai/auth")
@RequiredArgsConstructor
public class UserController {

    public final IUserService userService;

    @PostMapping(value = "/token", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    public Result<LoginResponse> login(@RequestParam("username") String username, @RequestParam("password") String password) {
        LoginRequest loginRequest = new LoginRequest(username,  password);
        return userService.login(loginRequest);
    }

    @PostMapping("/register")
    public Result<RegisterResponse> register(@RequestBody RegisterRequest registerRequest) {
        return userService.register(registerRequest);
    }
}
