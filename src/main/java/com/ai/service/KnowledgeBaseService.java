package com.ai.service;

import com.ai.entity.KnowledgeBase;
import com.ai.mapper.KnowledgeBaseMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class KnowledgeBaseService {

    @Autowired
    private KnowledgeBaseMapper knowledgeBaseMapper;

    /**
     * 获取所有知识库数据
     * @return 知识库数据列表
     */
    public List<KnowledgeBase> getAllKnowledge() {
        return knowledgeBaseMapper.selectList(null);
    }

    /**
     * 添加新知识到知识库
     * @param knowledgeBase 要添加的知识
     */
    public void addKnowledge(KnowledgeBase knowledgeBase) {
        knowledgeBase.setCreateTime(LocalDateTime.now());
        knowledgeBase.setUpdateTime(LocalDateTime.now());
        knowledgeBaseMapper.insert(knowledgeBase);
    }

    /**
     * 根据问题精确查找知识
     * @param question 要查找的问题
     * @return 匹配的知识，如果未找到则返回 null
     */
    public KnowledgeBase findKnowledgeByQuestion(String question) {
        List<KnowledgeBase> knowledgeBases = knowledgeBaseMapper.selectList(null);
        for (KnowledgeBase knowledgeBase : knowledgeBases) {
            if (question.equals(knowledgeBase.getQuestion())) {
                return knowledgeBase;
            }
        }
        return null;
    }

    /**
     * 根据内容查找知识
     * @param content 要查找的知识内容
     * @return 匹配的知识，如果未找到则返回 null
     */
    public KnowledgeBase findKnowledgeByContent(String content) {
        List<KnowledgeBase> knowledgeBases = knowledgeBaseMapper.selectList(null);
        for (KnowledgeBase knowledgeBase : knowledgeBases) {
            // 检查问题或答案中是否包含目标内容
            if ((knowledgeBase.getQuestion() != null && knowledgeBase.getQuestion().contains(content)) ||
                    (knowledgeBase.getAnswer() != null && knowledgeBase.getAnswer().contains(content))) {
                return knowledgeBase;
            }
        }
        return null;
    }

    /**
     * 根据 ID 更新知识库中的知识
     * @param knowledgeBase 包含更新信息的知识对象
     * @return 更新成功返回 true，失败返回 false
     */
    public boolean updateKnowledge(KnowledgeBase knowledgeBase) {
        knowledgeBase.setUpdateTime(LocalDateTime.now());
        int rows = knowledgeBaseMapper.updateById(knowledgeBase);
        return rows > 0;
    }

    /**
     * 根据 ID 删除知识库中的知识
     * @param id 要删除的知识的 ID
     * @return 删除成功返回 true，失败返回 false
     */
    public boolean deleteKnowledge(Long id) {
        int rows = knowledgeBaseMapper.deleteById(id);
        return rows > 0;
    }
}