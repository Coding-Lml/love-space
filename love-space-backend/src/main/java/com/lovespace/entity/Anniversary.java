package com.lovespace.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("anniversary")
public class Anniversary {
    
    @TableId(type = IdType.AUTO)
    private Long id;
    
    private Long spaceId;
    
    private String title;
    
    private String description;
    
    private LocalDate date;
    
    /** 类型: past(纪念日)/future(倒计时) */
    private String type;
    
    private Boolean repeatYearly;
    
    private Boolean remind;
    
    private Integer remindDays;
    
    private String icon;
    
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
    
    @TableLogic
    private Integer deleted;
    
    // 非数据库字段 - 计算天数
    @TableField(exist = false)
    private Long days;
    
    @TableField(exist = false)
    private String daysText;
}
