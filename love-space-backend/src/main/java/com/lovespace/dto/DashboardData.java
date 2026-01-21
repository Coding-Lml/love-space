package com.lovespace.dto;

import com.lovespace.entity.Anniversary;
import com.lovespace.entity.Moment;
import lombok.Data;
import java.util.List;

@Data
public class DashboardData {
    
    /** 在一起的天数 */
    private Long togetherDays;
    
    /** 在一起的小时数（当天） */
    private Long togetherHours;
    
    /** 在一起的分钟数（当天） */
    private Long togetherMinutes;
    
    /** 在一起的秒数（当天） */
    private Long togetherSeconds;
    
    /** 恋爱开始日期 */
    private String startDate;
    
    /** 格式化的时间文本 */
    private String togetherText;
    
    /** 即将到来的纪念日 */
    private List<Anniversary> upcomingAnniversaries;
    
    /** 最新动态 */
    private List<Moment> recentMoments;
}
