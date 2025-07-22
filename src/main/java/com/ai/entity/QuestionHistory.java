package com.ai.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("question_history")
public class QuestionHistory {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String userId; // 添加userIdStr字段
    private String sessionId;
    private String question;
    private String answer;
    private LocalDateTime createTime;

    // 添加setUserIdStr方法
    public void setUserId(String userIdStr) {
        this.userId = userIdStr;
    }
}