package com.typenglish.service;

import cn.hutool.crypto.digest.BCrypt;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.typenglish.common.BusinessException;
import com.typenglish.entity.User;
import com.typenglish.mapper.UserMapper;
import com.typenglish.security.JwtUtil;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    private final UserMapper userMapper;
    private final JwtUtil jwtUtil;

    public AuthService(UserMapper userMapper, JwtUtil jwtUtil) {
        this.userMapper = userMapper;
        this.jwtUtil = jwtUtil;
    }

    public record AuthResult(String token, User user) {}

    public AuthResult register(String username, String email, String password) {
        if (userMapper.selectOne(new LambdaQueryWrapper<User>().eq(User::getEmail, email)) != null) {
            throw new BusinessException("邮箱已被注册");
        }
        if (userMapper.selectOne(new LambdaQueryWrapper<User>().eq(User::getUsername, username)) != null) {
            throw new BusinessException("用户名已被占用");
        }

        User user = new User();
        user.setUsername(username);
        user.setEmail(email);
        user.setPassword(BCrypt.hashpw(password));
        userMapper.insert(user);

        String token = jwtUtil.generateToken(user.getId());
        user.setPassword(null);
        return new AuthResult(token, user);
    }

    /** 支持用户名或邮箱登录 */
    public AuthResult login(String account, String password) {
        User user = userMapper.selectOne(
                new LambdaQueryWrapper<User>()
                        .eq(User::getUsername, account)
                        .or()
                        .eq(User::getEmail, account)
        );
        if (user == null) {
            throw new BusinessException("账号或密码错误");
        }
        if (!BCrypt.checkpw(password, user.getPassword())) {
            throw new BusinessException("账号或密码错误");
        }

        String token = jwtUtil.generateToken(user.getId());
        user.setPassword(null);
        return new AuthResult(token, user);
    }
}
