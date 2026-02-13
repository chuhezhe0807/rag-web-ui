package com.chuhezhe.raguserservice.controller;

import com.chuhezhe.common.entity.Result;
import com.chuhezhe.raguserservice.dto.UserLoginDTO;
import com.chuhezhe.raguserservice.dto.UserRegisterDTO;
import com.chuhezhe.raguserservice.service.IUserService;
import com.chuhezhe.raguserservice.vo.UserLoginVo;
import com.chuhezhe.raguserservice.vo.UserRegisterVO;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@Validated
@RestController
@RequestMapping("/api/ai/auth")
@RequiredArgsConstructor
public class UserController {

    public final IUserService userService;

    @PostMapping(value = "/token", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    public Result<UserLoginVo> login(
            @RequestParam("username")
            @NotBlank(message = "{validation.adminUser.username.notNull}")
            @Size(min = 3, max = 50, message = "{validation.adminUser.username.size}")
            String username,

            @RequestParam("password")
            @NotBlank(message = "{validation.adminUser.password.notBlank}")
            @Pattern(
                    regexp = "^(?=.*[A-Z])(?=.*[a-z])(?=.*\\d).{8,}$",
                    message = "{validation.adminUser.password.pattern}"
            )
            String password
    ) {
        UserLoginDTO loginDTO = new UserLoginDTO(username,  password);
        return userService.login(loginDTO);
    }

    @PostMapping("/register")
    public Result<UserRegisterVO> register(@Valid @RequestBody UserRegisterDTO registerRequest) {
        return userService.register(registerRequest);
    }
}
