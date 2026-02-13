package com.chuhezhe.raguserservice.controller;

import com.chuhezhe.common.entity.Result;
import com.chuhezhe.raguserservice.dto.UserLoginDTO;
import com.chuhezhe.raguserservice.dto.UserRegisterDTO;
import com.chuhezhe.raguserservice.service.IUserService;
import com.chuhezhe.raguserservice.vo.UserLoginVo;
import com.chuhezhe.raguserservice.vo.UserRegisterVO;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/ai/auth")
@RequiredArgsConstructor
public class UserController {

    public final IUserService userService;

    @PostMapping(value = "/token", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    public Result<UserLoginVo> login(@RequestParam("username") String username, @RequestParam("password") String password) {
        UserLoginDTO loginDTO = new UserLoginDTO(username,  password);
        return userService.login(loginDTO);
    }

    @PostMapping("/register")
    public Result<UserRegisterVO> register(@RequestBody UserRegisterDTO registerRequest) {
        return userService.register(registerRequest);
    }
}
