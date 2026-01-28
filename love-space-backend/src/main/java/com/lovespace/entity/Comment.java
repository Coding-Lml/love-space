package com.lovespace.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("comment")
public class Comment {
    
    @TableId(type = IdType.AUTO)
    private Long id;
    
    private Long momentId;
    
    private Long userId;
    
    private Long parentId;

    private Long replyToUserId;

    private String content;
    
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    
    @TableLogic
    private Integer deleted;
    
    // 非数据库字段
    @TableField(exist = false)
    private User user;

    @TableField(exist = false)
    private User replyToUser;
}
