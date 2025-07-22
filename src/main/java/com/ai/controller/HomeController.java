package com.ai.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController  // 标记为控制器，返回 JSON 响应
public class HomeController {

    // 映射根路径（对应 http://localhost:8080/api）
    @GetMapping("/")
    public String home() {
        return "AI 客服系统 API 根路径，运行正常！";
    }
}