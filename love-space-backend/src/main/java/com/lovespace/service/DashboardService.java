package com.lovespace.service;

import com.lovespace.dto.DashboardData;
import com.lovespace.entity.Anniversary;
import com.lovespace.entity.Moment;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
@RequiredArgsConstructor
public class DashboardService {
    
    @Value("${couple.start-date}")
    private String coupleStartDate;
    
    private final AnniversaryService anniversaryService;
    private final MomentService momentService;
    
    /**
     * 获取首页仪表盘数据
     */
    public DashboardData getDashboardData(Long currentUserId) {
        DashboardData data = new DashboardData();
        
        // 计算在一起的时间
        LocalDate startDate = LocalDate.parse(coupleStartDate);
        LocalDateTime startDateTime = LocalDateTime.of(startDate, LocalTime.MIDNIGHT);
        LocalDateTime now = LocalDateTime.now();
        
        // 计算总天数
        long totalDays = ChronoUnit.DAYS.between(startDate, LocalDate.now());
        data.setTogetherDays(totalDays);
        data.setStartDate(coupleStartDate);
        
        // 计算当天的小时、分钟、秒
        Duration duration = Duration.between(startDateTime, now);
        long totalSeconds = duration.getSeconds();
        long hours = (totalSeconds % 86400) / 3600;
        long minutes = (totalSeconds % 3600) / 60;
        long seconds = totalSeconds % 60;
        
        data.setTogetherHours(hours);
        data.setTogetherMinutes(minutes);
        data.setTogetherSeconds(seconds);
        
        // 格式化文本
        data.setTogetherText(formatTogetherText(totalDays, hours, minutes));
        
        // 获取即将到来的纪念日
        List<Anniversary> upcomingAnniversaries = anniversaryService.getUpcoming(currentUserId, 30);
        data.setUpcomingAnniversaries(upcomingAnniversaries);
        
        // 获取最新动态
        List<Moment> recentMoments = momentService.getRecentMoments(currentUserId, 5);
        data.setRecentMoments(recentMoments);
        
        return data;
    }

    public DashboardData getHostDashboardData(Long hostUserId, Long viewerUserId) {
        DashboardData data = new DashboardData();

        LocalDate startDate = LocalDate.parse(coupleStartDate);
        LocalDateTime startDateTime = LocalDateTime.of(startDate, LocalTime.MIDNIGHT);
        LocalDateTime now = LocalDateTime.now();

        long totalDays = ChronoUnit.DAYS.between(startDate, LocalDate.now());
        data.setTogetherDays(totalDays);
        data.setStartDate(coupleStartDate);

        Duration duration = Duration.between(startDateTime, now);
        long totalSeconds = duration.getSeconds();
        long hours = (totalSeconds % 86400) / 3600;
        long minutes = (totalSeconds % 3600) / 60;
        long seconds = totalSeconds % 60;

        data.setTogetherHours(hours);
        data.setTogetherMinutes(minutes);
        data.setTogetherSeconds(seconds);
        data.setTogetherText(formatTogetherText(totalDays, hours, minutes));

        List<Anniversary> upcomingAnniversaries = anniversaryService.getUpcoming(hostUserId, 30);
        data.setUpcomingAnniversaries(upcomingAnniversaries);

        List<Moment> recentMoments = momentService.getRecentPublicMoments(hostUserId, viewerUserId, 5);
        data.setRecentMoments(recentMoments);

        return data;
    }
    
    /**
     * 格式化在一起时间文本
     */
    private String formatTogetherText(long days, long hours, long minutes) {
        if (days == 0) {
            return String.format("在一起 %d 小时 %d 分钟", hours, minutes);
        } else if (days < 30) {
            return String.format("在一起 %d 天 %d 小时", days, hours);
        } else if (days < 365) {
            long months = days / 30;
            long remainDays = days % 30;
            return String.format("在一起 %d 个月 %d 天", months, remainDays);
        } else {
            long years = days / 365;
            long remainDays = days % 365;
            long months = remainDays / 30;
            return String.format("在一起 %d 年 %d 个月", years, months);
        }
    }
}
