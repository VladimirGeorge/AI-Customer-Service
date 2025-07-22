package com.ai.controller;

import com.ai.entity.QuestionHistory;
import com.ai.entity.KnowledgeBase;
import com.ai.mapper.ConversationMapper;
import com.ai.mapper.QuestionHistoryMapper;
import com.ai.common.Result;
import com.ai.service.CacheService;
import com.ai.service.ModelService;
import com.ai.service.KnowledgeBaseService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
public class ChatController {
    private static final Logger logger = LoggerFactory.getLogger(com.ai.controller.ChatController.class);
    private final ModelService modelService;
    private final CacheService cacheService;
    private final QuestionHistoryMapper historyMapper;
    private final ConversationMapper conversationMapper;
    private final KnowledgeBaseService knowledgeBaseService;

    /**
     * 流式问答接口（SSE）
     */
    @PostMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public void streamAsk(@RequestBody Map<String, String> params,
                          @RequestParam(defaultValue = "qwen") String modelType, // 添加模型类型参数
                          HttpServletResponse response) throws IOException {
        logger.info("收到流式请求: {}", params);

        // 参数校验
        String question = params.get("question");
        String sessionId = params.get("sessionId");
        String userId = params.get("userId");

        if (question == null || question.trim().isEmpty()) {
            logger.warn("问题不能为空");
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().write("data: [ERROR] 问题不能为空\n\n");
            return;
        }

        if (sessionId == null || sessionId.trim().isEmpty()) {
            logger.warn("会话ID不能为空");
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().write("data: [ERROR] 会话ID不能为空\n\n");
            return;
        }

        if (userId == null || userId.trim().isEmpty()) {
            logger.warn("用户ID不能为空");
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().write("data: [ERROR] 用户ID不能为空\n\n");
            return;
        }

        // 1. 查缓存
        String cachedAnswer = cacheService.get(question);
        if (cachedAnswer != null) {
            // 去除 [START] 和 [END] 标记
            cachedAnswer = cachedAnswer.replace("[START]", "").replace("[END]", "").trim();
            logger.info("命中缓存: sessionId={}, question={}", sessionId, question);
            // 流式输出缓存答案
            response.setContentType(MediaType.TEXT_EVENT_STREAM_VALUE);
            response.setCharacterEncoding("UTF-8");
            response.setHeader("Cache-Control", "no-cache");
            response.setHeader("Connection", "keep-alive");
            try (var writer = response.getWriter()) {
                for (char c : cachedAnswer.toCharArray()) {
                    writer.write("data: " + c + "\n\n");
                    writer.flush();
                }
                saveHistoryAndReturn(userId, sessionId, question, cachedAnswer);
            } catch (Exception e) {
                logger.error("流式输出缓存答案失败: {}", e.getMessage(), e);
                response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                response.getWriter().write("data: [ERROR] " + e.getMessage() + "\n\n");
            }
            return;
        }

        // 2. 查知识库
        KnowledgeBase knowledge = knowledgeBaseService.findKnowledgeByQuestion(question);
        if (knowledge != null) {
            String answer = knowledge.getAnswer();
            cacheService.put(question, answer); // 存入缓存
            logger.info("命中知识库: sessionId={}, question={}", sessionId, question);
            // 流式输出知识库答案
            response.setContentType(MediaType.TEXT_EVENT_STREAM_VALUE);
            response.setCharacterEncoding("UTF-8");
            response.setHeader("Cache-Control", "no-cache");
            response.setHeader("Connection", "keep-alive");
            try (var writer = response.getWriter()) {
                for (char c : answer.toCharArray()) {
                    writer.write("data: " + c + "\n\n");
                    writer.flush();
                }
                saveHistoryAndReturn(userId, sessionId, question, answer);
            } catch (Exception e) {
                logger.error("流式输出知识库答案失败: {}", e.getMessage(), e);
                response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                response.getWriter().write("data: [ERROR] " + e.getMessage() + "\n\n");
            }
            return;
        }

        // 3. 调用大模型
        logger.info("调用大模型: sessionId={}, question={}", sessionId, question);
        // 设置响应头
        response.setContentType(MediaType.TEXT_EVENT_STREAM_VALUE);
        response.setCharacterEncoding("UTF-8");
        response.setHeader("Cache-Control", "no-cache");
        response.setHeader("Connection", "keep-alive");

        StringBuilder answerBuilder = new StringBuilder();
        try (var writer = response.getWriter()) {
            if ("deepseek".equals(modelType)) {
                // 调用DeepSeek模型的流式方法
                modelService.streamCallOllamaModel(question, chunk -> {
                    // 去除 [START] 和 [END] 标记
                    String cleanedChunk = chunk.replace("[START]", "").replace("[END]", "").trim();
                    if (!cleanedChunk.isEmpty()) {
                        answerBuilder.append(cleanedChunk);
                        for (char c : cleanedChunk.toCharArray()) {
                            writer.write("data: " + c + "\n\n");
                            writer.flush();
                        }
                    }
                });
            } else {
                // 调用Qwen模型的流式方法
                modelService.streamCallModel(question, chunk -> {
                    // 去除 [START] 和 [END] 标记
                    String cleanedChunk = chunk.replace("[START]", "").replace("[END]", "").trim();
                    if (!cleanedChunk.isEmpty()) {
                        answerBuilder.append(cleanedChunk);
                        for (char c : cleanedChunk.toCharArray()) {
                            writer.write("data: " + c + "\n\n");
                            writer.flush();
                        }
                    }
                });
            }

            String answer = answerBuilder.toString();
            cacheService.put(question, answer); // 存入缓存
            saveHistoryAndReturn(userId, sessionId, question, answer);

            logger.info("流式请求处理完成: sessionId={}, question={}", sessionId, question);
        } catch (Exception e) {
            logger.error("流式请求处理失败: {}", e.getMessage(), e);
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            response.getWriter().write("data: [ERROR] " + e.getMessage() + "\n\n");
        }
    }

    /**
     * 非流式问答接口
     */
    @PostMapping("/ask")
    public Result<QuestionHistory> ask(@RequestBody Map<String, String> params, @RequestParam(defaultValue = "qwen") String modelType) {
        logger.info("收到非流式请求: {}", params);

        // 参数校验
        String userId = params.get("userId");
        String sessionId = params.get("sessionId");
        String question = params.get("question");

        if (userId == null || userId.trim().isEmpty()) {
            logger.warn("用户ID不能为空");
            return Result.error(400, "用户ID不能为空");
        }

        // 1. 查缓存
        String cachedAnswer = cacheService.get(question);
        if (cachedAnswer != null) {
            // 去除 [START] 和 [END] 标记
            cachedAnswer = cachedAnswer.replace("[START]", "").replace("[END]", "").trim();
            logger.info("命中缓存: sessionId={}, question={}", sessionId, question);
            return saveHistoryAndReturn(userId, sessionId, question, cachedAnswer);
        }

        // 2. 查知识库
        KnowledgeBase knowledge = knowledgeBaseService.findKnowledgeByQuestion(question);
        if (knowledge != null) {
            String answer = knowledge.getAnswer();
            cacheService.put(question, answer); // 存入缓存
            logger.info("命中知识库: sessionId={}, question={}", sessionId, question);
            return saveHistoryAndReturn(userId, sessionId, question, answer);
        }

        // 3. 调用大模型
        logger.info("调用大模型: sessionId={}, question={}", sessionId, question);
        String answer;
        if ("deepseek".equals(modelType)) {
            answer = modelService.callOllamaModel(question);
        } else {
            answer = modelService.callModel(question);
        }
        // 去除 [START] 和 [END] 标记
        answer = answer.replace("[START]", "").replace("[END]", "").trim();
        cacheService.put(question, answer); // 存入缓存
        return saveHistoryAndReturn(userId, sessionId, question, answer);
    }

    /**
     * 保存聊天历史并返回结果
     */
    private Result<QuestionHistory> saveHistoryAndReturn(String userId, String sessionId, String question, String answer) {
        QuestionHistory history = new QuestionHistory();
        history.setUserId(userId);
        history.setSessionId(sessionId);
        history.setQuestion(question);
        history.setAnswer(answer);
        history.setCreateTime(LocalDateTime.now());

        try {
            historyMapper.insert(history);
            logger.info("保存聊天历史成功: id={}, sessionId={}", history.getId(), sessionId);
        } catch (Exception e) {
            logger.error("保存聊天历史失败: {}", e.getMessage(), e);
            // 即使保存失败，也返回结果给用户
        }

        return Result.success(history);
    }

    /**
     * 获取问答历史记录
     */
    @GetMapping("/history")
    public Result<List<QuestionHistory>> getQuestionHistory(@RequestHeader("Authorization") String token) {
        try {
            // 验证token逻辑...
            List<QuestionHistory> historyList = historyMapper.selectList(null);
            return Result.success(historyList); // 确保返回 Result 对象
        } catch (Exception e) {
            logger.error("获取问答历史记录失败: {}", e.getMessage(), e);
            return Result.error(500, "获取问答历史记录失败: " + e.getMessage());
        }
    }
}