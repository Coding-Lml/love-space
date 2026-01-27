package com.lovespace.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

@Data
@TableName("moment")
public class Moment {
    
    @TableId(type = IdType.AUTO)
    private Long id;
    
    private Long spaceId;
    
    private Long userId;
    
    private String content;
    
    private String location;
    
    private Integer likes;

    private String visibility;
    
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
    
    @TableLogic
    private Integer deleted;
    
    // 非数据库字段
    @TableField(exist = false)
    private User user;
    
    @TableField(exist = false)
    private List<MomentMedia> mediaList;
    
    @TableField(exist = false)
    private List<Comment> comments;
    
    @TableField(exist = false)
    private Boolean liked;
}
