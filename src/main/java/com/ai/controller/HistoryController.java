package com.ai.controller;

import com.ai.entity.QuestionHistory;
import com.ai.mapper.QuestionHistoryMapper;
import com.ai.common.Result;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/history")
public class HistoryController {

    @Autowired
    private QuestionHistoryMapper historyMapper;

    /**
     * 根据用户ID获取历史记录
     */
    @GetMapping("/user/{userId}")
    public Result<List<QuestionHistory>> getHistoryByUserId(@PathVariable String userId) {
        List<QuestionHistory> historyList = historyMapper.selectList(
                Wrappers.<QuestionHistory>lambdaQuery()
                        .eq(QuestionHistory::getUserId, userId)
                        .orderByDesc(QuestionHistory::getCreateTime)
        );
        return Result.success(historyList);
    }

    /**
     * 根据会话ID获取历史记录
     */
    @GetMapping("/session/{sessionId}")
    public Result<List<QuestionHistory>> getHistoryBySessionId(@PathVariable String sessionId) {
        List<QuestionHistory> historyList = historyMapper.selectList(
                Wrappers.<QuestionHistory>lambdaQuery()
                        .eq(QuestionHistory::getSessionId, sessionId)
                        .orderByAsc(QuestionHistory::getCreateTime)
        );
        return Result.success(historyList);
    }
}