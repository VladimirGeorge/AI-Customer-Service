package com.ai.service;

import com.ai.entity.User;
import com.ai.mapper.UserMapper;
import com.ai.util.JwtUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.userdetails.MapReactiveUserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.security.SignatureException;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j // 添加日志记录
public class AuthService {
    private final UserMapper userMapper;
    private final BCryptPasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    // 注册：使用邮箱作为用户名，强化参数校验和异常信息
    public User register(String email, String password) {
        // 1. 基础参数校验（后端二次校验，防止前端绕过）
        if (!StringUtils.hasText(email) || !email.matches("^[\\w-\\.]+@([\\w-]+\\.)+[\\w-]{2,4}$")) {
            log.error("注册失败：邮箱格式无效，输入值={}", email);
            throw new IllegalArgumentException("请输入有效的邮箱地址"); // 使用IllegalArgumentException明确参数错误
        }

        if (!StringUtils.hasText(password) || password.length() < 8
                || !password.matches(".*[A-Za-z].*") || !password.matches(".*\\d.*")) {
            log.error("注册失败：密码格式无效");
            throw new IllegalArgumentException("密码需至少8位，包含字母和数字");
        }

        // 2. 检查邮箱是否已注册
        LambdaQueryWrapper<User> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(User::getUsername, email);
        boolean isExist = userMapper.exists(queryWrapper);
        if (isExist) {
            log.error("注册失败：邮箱已被注册，邮箱={}", email);
            throw new RuntimeException("该邮箱已被注册"); // 业务异常，明确提示
        }

        // 3. 创建用户（邮箱存储在username字段）
        User user = new User();
        user.setUsername(email); // 邮箱作为用户名
        user.setPassword(passwordEncoder.encode(password)); // 密码加密
        user.setCreateTime(LocalDateTime.now());
        user.setUpdateTime(LocalDateTime.now());

        // 4. 保存用户（捕获数据库操作异常）
        try {
            userMapper.insert(user);
            log.info("注册成功：邮箱={}", email);
            return user;
        } catch (Exception e) {
            log.error("注册失败：数据库保存异常，邮箱={}", email, e);
            throw new RuntimeException("注册失败，请稍后重试"); // 隐藏数据库细节，返回友好提示
        }
    }

    // 登录：优化异常信息和日志
    public String login(String email, String password) {
        // 1. 基础参数校验
        if (!StringUtils.hasText(email) || !StringUtils.hasText(password)) {
            log.error("登录失败：邮箱或密码为空");
            throw new IllegalArgumentException("邮箱和密码不能为空");
        }

        // 2. 根据邮箱查询用户
        LambdaQueryWrapper<User> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(User::getUsername, email);
        User user = userMapper.selectOne(queryWrapper);

        // 3. 校验用户和密码
        if (user == null) {
            log.error("登录失败：用户不存在，邮箱={}", email);
            throw new RuntimeException("用户名或密码错误"); // 统一错误提示，避免暴露用户是否存在
        }

        if (!passwordEncoder.matches(password, user.getPassword())) {
            log.error("登录失败：密码错误，邮箱={}", email);
            throw new RuntimeException("用户名或密码错误");
        }

        // 4. 生成Token
        try {
            // 1. 校验用户ID和用户名是否为空（核心参数校验）
            if (user.getId() == null) {
                log.error("登录失败：用户ID为空，用户名={}", email);
                throw new RuntimeException("系统异常：用户信息不完整");
            }
            if (user.getUsername() == null) {
                log.error("登录失败：用户名为空，用户ID={}", user.getId());
                throw new RuntimeException("系统异常：用户信息不完整");
            }

            // 2. 生成Token（捕获具体异常类型）
            String token;
            try {
                token = jwtUtil.generateToken(user.getId(), user.getUsername());
            } catch (ExpiredJwtException e) {
                log.error("登录失败：JWT已过期（通常是生成时过期时间设置错误），用户名={}", email, e);
                throw new RuntimeException("登录失败：Token生成异常");
            } catch (IllegalArgumentException e) {
                log.error("登录失败：JWT参数为空（userId或username为null），用户名={}", email, e);
                throw new RuntimeException("登录失败：用户信息不完整");
            } catch (Exception e) {
                log.error("登录失败：Token生成未知异常，用户名={}", email, e);
                throw new RuntimeException("登录失败：系统异常，请联系管理员");
            }

            log.info("登录成功：邮箱={}，生成Token：{}", email, token.substring(0, 20) + "..."); // 只打印部分Token
            return token;
        } catch (RuntimeException e) {
            // 保留原有业务异常（如用户不存在、密码错误）
            throw e;
        } catch (Exception e) {
            log.error("登录失败：系统未知异常，用户名={}", email, e);
            throw new RuntimeException("登录失败，请稍后重试");
        }
    }
    // 根据用户ID查询用户（需注入UserMapper或UserRepository）
    public User getUserById(Long userId) {
        return userMapper.selectById(userId); // 以MyBatis为例
    }

}