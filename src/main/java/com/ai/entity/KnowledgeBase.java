package com.ai.entity;// com.ai.entity.KnowledgeBase
import java.time.LocalDateTime;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableField;
import lombok.Data;

@Data  // 必须有lombok的@Data注解（或手动生成getter/setter）
public class KnowledgeBase {
    @TableId(type = IdType.AUTO)
    private Long id;

    // 与前端传递的参数名一致：question和answer
    @TableField("question")
    private String question;

    @TableField("answer")
    private String answer;

    @TableField("create_time")
    private LocalDateTime createTime;

    @TableField("update_time")
    private LocalDateTime updateTime;
}