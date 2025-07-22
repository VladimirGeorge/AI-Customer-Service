package com.ai.controller;

import com.ai.entity.KnowledgeBase;
import com.ai.service.KnowledgeBaseService;
import com.ai.common.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/knowledge")
public class KnowledgeBaseController {

    @Autowired
    private KnowledgeBaseService knowledgeBaseService;

    /**
     * 获取所有知识库数据
     */
    @GetMapping("/base")
    public Result<List<KnowledgeBase>> getAllKnowledge() {
        List<KnowledgeBase> knowledgeList = knowledgeBaseService.getAllKnowledge();
        return Result.success(knowledgeList);
    }

    /**
     * 添加新知识到知识库
     */
    @PostMapping("/base")
    public Result<String> addKnowledge(@RequestBody KnowledgeBase knowledgeBase) {
        knowledgeBaseService.addKnowledge(knowledgeBase);
        return Result.success("添加知识成功");
    }

    /**
     * 根据ID删除知识库中的知识
     */
    @DeleteMapping("/base/{id}")
    public Result<String> deleteKnowledge(@PathVariable Long id) {
        boolean success = knowledgeBaseService.deleteKnowledge(id);
        if (success) {
            return Result.success("删除知识成功");
        } else {
            return Result.error(400, "删除知识失败");
        }
    }
}