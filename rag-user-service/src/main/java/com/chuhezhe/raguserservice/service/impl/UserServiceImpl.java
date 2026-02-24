package com.chuhezhe.raguserservice.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.chuhezhe.common.constants.ErrorConstants;
import com.chuhezhe.common.constants.GConstants;
import com.chuhezhe.common.entity.Result;
import com.chuhezhe.common.util.BcryptUtil;
import com.chuhezhe.common.util.JWTUtil;
import com.chuhezhe.raguserservice.dto.UserLoginDTO;
import com.chuhezhe.raguserservice.dto.UserRegisterDTO;
import com.chuhezhe.raguserservice.entity.User;
import com.chuhezhe.raguserservice.mapper.user.UserMapper;
import com.chuhezhe.raguserservice.service.IUserService;
import com.chuhezhe.raguserservice.vo.UserLoginVO;
import com.chuhezhe.raguserservice.vo.UserRegisterVO;
import com.chuhezhe.raguserservice.vo.UserVO;
import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * 用户服务实现类
 * 改用java服务器实现，不再使用feign
 */
@Service
@RequiredArgsConstructor
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements IUserService {

    @Override
    public Result<UserLoginVO> login(UserLoginDTO userLoginDTO) {
        // 1. 查询用户是否存在
        User user = baseMapper.getUserByUsername(userLoginDTO.getUsername());

        if(user == null) {
            return Result.error(ErrorConstants.USER_NOT_EXIST);
        }
        else if(!BcryptUtil.verifyPassword(userLoginDTO.getPassword(), user.getHashedPassword())) { // 2. 密码是否正确
            return Result.error(ErrorConstants.LOGGING_ERROR);
        }
        else if(!user.isActive()) { // 3. 验证用户是否被禁用
            return Result.error(ErrorConstants.USER_DISABLED);
        }

        // 4. 生成token并返回
        UserLoginVO userLoginVo = new UserLoginVO();
        userLoginVo.setAccessToken(JWTUtil.generateToken(Map.of(GConstants.JWT_TOKEN_USERNAME_KEY, user.getUsername()))); // 与python服务保持一致
        userLoginVo.setTokenType("Bearer");

        return Result.ok(userLoginVo);
    }

    @Override
    public Result<UserRegisterVO> register(UserRegisterDTO dto) {
        // 1. 验证用户名/邮箱是否已存在
        if (baseMapper.getUserByUsername(dto.getUsername()) != null) {
            return Result.error(ErrorConstants.USERNAME_ALREADY_EXIST);
        }
        else if (baseMapper.getUserByEmail(dto.getEmail()) != null) {
            return Result.error(ErrorConstants.EMAIL_ALREADY_EXIST);
        }

        // 2. 创建用户
        User user = User.builder()
                .username(dto.getUsername())
                .email(dto.getEmail())
                .hashedPassword(BcryptUtil.getPasswordHash(dto.getPassword()))
                .isActive(true)
                .isSuperuser(false)
                .build();
        baseMapper.insert(user);

        return Result.ok(
                new UserRegisterVO(
                        user.getEmail(),
                        user.getUsername(),
                        user.isActive(),
                        user.isSuperuser(),
                        user.getId()
                )
        );
    }

    @Override
    public Result<UserVO> getUserInfo(String token) {
        // 1. 校验token
        if (!JWTUtil.validateToken(token)) {
            return Result.error(ErrorConstants.UNAUTHORIZED);
        }

        // 2. 解析token
        Claims claims = JWTUtil.getClaims(token);
        String username;

        if(claims == null || (username = claims.get(GConstants.JWT_TOKEN_USERNAME_KEY).toString()) == null) {
            return Result.error(ErrorConstants.UNAUTHORIZED);
        }

        // 3. 查询用户
        User user = baseMapper.getUserByUsername(username);

        if(user == null) {
            return Result.error(ErrorConstants.USER_NOT_EXIST);
        }

        return Result.ok(
                new UserVO(
                        user.getId(),
                        user.getUsername(),
                        user.getEmail(),
                        user.isActive(),
                        user.isSuperuser()
                )
        );
    }
}
