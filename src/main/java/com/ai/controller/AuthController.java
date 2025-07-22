package com.ai.controller;

import com.ai.entity.User;
import com.ai.service.AuthService;
import com.ai.common.Result;
import com.ai.util.JwtUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {
    private final AuthService authService;
    private final JwtUtil jwtUtil;

    // 注册接口
    @PostMapping("/register")
    public Result<User> register(@Valid @RequestBody RegisterRequest request) {
        try {
            // 调用服务层注册方法
            User user = authService.register(request.getUsername(), request.getPassword());
            return Result.success(user);
        } catch (Exception e) {
            // 注册失败，返回错误信息
            return Result.error(500, "注册失败: " + e.getMessage());
        }
    }

    // 登录接口
    @PostMapping("/login")
    public Result<String> login(@Valid @RequestBody LoginRequest request) {
        try {
            // 调用服务层登录方法，获取Token
            String token = authService.login(request.getUsername(), request.getPassword());
            return Result.success(token);
        } catch (Exception e) {
            // 登录失败，返回错误信息
            return Result.error(401, "登录失败: " + e.getMessage());
        }
    }

    // 验证Token有效性接口
    @GetMapping("/verify")
    public Result<?> verifyToken(HttpServletRequest request) {
        try {
            String authHeader = request.getHeader("Authorization");
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                return Result.error(401, "缺少Token");
            }

            String token = authHeader.substring(7);
            boolean isValid = jwtUtil.verifyToken(token);

            if (isValid) {
                return Result.success("Token有效");
            } else {
                return Result.error(401, "Token无效或已过期");
            }
        } catch (Exception e) {
            return Result.error(500, "Token验证失败: " + e.getMessage());
        }
    }

    // 获取用户信息接口
    @GetMapping("/userinfo")
    public Result<?> getUserInfo(HttpServletRequest request) {
        try {
            String authHeader = request.getHeader("Authorization");
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                return Result.error(401, "缺少Token");
            }

            String token = authHeader.substring(7);
            if (!jwtUtil.verifyToken(token)) {
                return Result.error(401, "无效Token");
            }

            // 从Token中提取用户ID
            Long userId = jwtUtil.getUserIdFromToken(token);
            // 根据用户ID查询完整用户信息
            User user = authService.getUserById(userId);

            if (user == null) {
                return Result.error(404, "用户不存在");
            }

            return Result.success(user);
        } catch (Exception e) {
            return Result.error(500, "获取用户信息失败: " + e.getMessage());
        }
    }

    // 注册请求DTO
    @Data
    public static class RegisterRequest {
        @NotBlank(message = "邮箱不能为空")
        @Email(message = "邮箱格式不正确")
        private String username;

        @NotBlank(message = "密码不能为空")
        @Pattern(
                regexp = "^(?=.*[A-Za-z])(?=.*\\d)[A-Za-z\\d]{8,}$",
                message = "密码需至少8位，包含字母和数字"
        )
        private String password;
    }

    // 登录请求DTO
    @Data
    public static class LoginRequest {
        @NotBlank(message = "用户名（邮箱）不能为空")
        private String username;

        @NotBlank(message = "密码不能为空")
        private String password;
    }
}