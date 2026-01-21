package com.lovespace.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("moment_media")
public class MomentMedia {
    
    @TableId(type = IdType.AUTO)
    private Long id;
    
    private Long momentId;
    
    /** 类型: image/video */
    private String type;
    
    private String url;
    
    private String thumbnail;
    
    private Integer sort;
    
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
