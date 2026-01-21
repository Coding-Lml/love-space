package com.lovespace.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.lovespace.common.Result;
import com.lovespace.entity.Anniversary;
import com.lovespace.mapper.AnniversaryMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AnniversaryService extends ServiceImpl<AnniversaryMapper, Anniversary> {
    
    @Value("${couple.start-date}")
    private String coupleStartDate;
    
    /**
     * 添加纪念日
     */
    public Result<Anniversary> add(Anniversary anniversary) {
        this.save(anniversary);
        calculateDays(anniversary);
        return Result.success("添加成功", anniversary);
    }
    
    /**
     * 获取所有纪念日
     */
    public Result<List<Anniversary>> getAll() {
        List<Anniversary> list = this.list(new LambdaQueryWrapper<Anniversary>()
                .orderByAsc(Anniversary::getDate));
        
        for (Anniversary anniversary : list) {
            calculateDays(anniversary);
        }
        
        return Result.success(list);
    }
    
    /**
     * 获取即将到来的纪念日（未来30天内）
     */
    public List<Anniversary> getUpcoming(int days) {
        LocalDate today = LocalDate.now();
        List<Anniversary> allList = this.list();
        
        return allList.stream()
                .peek(this::calculateDays)
                .filter(a -> {
                    // 对于每年重复的纪念日，计算今年的日期
                    LocalDate targetDate = a.getDate();
                    if (Boolean.TRUE.equals(a.getRepeatYearly())) {
                        targetDate = targetDate.withYear(today.getYear());
                        // 如果今年的日期已过，看明年的
                        if (targetDate.isBefore(today)) {
                            targetDate = targetDate.plusYears(1);
                        }
                    }
                    long daysUntil = ChronoUnit.DAYS.between(today, targetDate);
                    return daysUntil >= 0 && daysUntil <= days;
                })
                .sorted(Comparator.comparing(a -> {
                    LocalDate targetDate = a.getDate();
                    if (Boolean.TRUE.equals(a.getRepeatYearly())) {
                        targetDate = targetDate.withYear(today.getYear());
                        if (targetDate.isBefore(today)) {
                            targetDate = targetDate.plusYears(1);
                        }
                    }
                    return targetDate;
                }))
                .limit(5)
                .collect(Collectors.toList());
    }
    
    /**
     * 更新纪念日
     */
    public Result<Anniversary> update(Anniversary anniversary) {
        Anniversary exist = this.getById(anniversary.getId());
        if (exist == null) {
            return Result.error("纪念日不存在");
        }
        
        this.updateById(anniversary);
        calculateDays(anniversary);
        return Result.success("更新成功", anniversary);
    }
    
    /**
     * 删除纪念日
     */
    public Result<Void> delete(Long id) {
        Anniversary anniversary = this.getById(id);
        if (anniversary == null) {
            return Result.error("纪念日不存在");
        }
        
        // 不允许删除"在一起"这个纪念日
        if ("在一起".equals(anniversary.getTitle())) {
            return Result.error("恋爱纪念日不能删除");
        }
        
        this.removeById(id);
        return Result.success("删除成功", null);
    }
    
    /**
     * 获取在一起的天数详情
     */
    public Result<Anniversary> getTogetherDays() {
        LocalDate startDate = LocalDate.parse(coupleStartDate);
        LocalDate today = LocalDate.now();
        long days = ChronoUnit.DAYS.between(startDate, today);
        
        Anniversary anniversary = new Anniversary();
        anniversary.setTitle("在一起");
        anniversary.setDate(startDate);
        anniversary.setType("past");
        anniversary.setIcon("💕");
        anniversary.setDays(days);
        anniversary.setDaysText(formatDaysText(days));
        
        return Result.success(anniversary);
    }
    
    /**
     * 计算天数
     */
    private void calculateDays(Anniversary anniversary) {
        LocalDate today = LocalDate.now();
        LocalDate targetDate = anniversary.getDate();
        
        if ("past".equals(anniversary.getType())) {
            // 纪念日：计算已经过了多少天
            if (Boolean.TRUE.equals(anniversary.getRepeatYearly())) {
                // 每年重复，计算今年的日期
                LocalDate thisYear = targetDate.withYear(today.getYear());
                if (thisYear.isAfter(today)) {
                    // 今年还没到，显示距离今年还有多少天
                    long daysUntil = ChronoUnit.DAYS.between(today, thisYear);
                    anniversary.setDays(daysUntil);
                    anniversary.setDaysText("还有" + daysUntil + "天");
                } else {
                    // 今年已过，计算从今年的日期到今天
                    long daysSince = ChronoUnit.DAYS.between(thisYear, today);
                    if (daysSince == 0) {
                        anniversary.setDays(0L);
                        anniversary.setDaysText("就是今天！");
                    } else {
                        // 计算距离明年还有多少天
                        LocalDate nextYear = thisYear.plusYears(1);
                        long daysUntil = ChronoUnit.DAYS.between(today, nextYear);
                        anniversary.setDays(daysUntil);
                        anniversary.setDaysText("还有" + daysUntil + "天");
                    }
                }
            } else {
                // 不重复，计算从那天到今天
                long days = ChronoUnit.DAYS.between(targetDate, today);
                anniversary.setDays(days);
                anniversary.setDaysText(formatDaysText(days));
            }
        } else {
            // 倒计时：计算还有多少天
            long days = ChronoUnit.DAYS.between(today, targetDate);
            anniversary.setDays(days);
            if (days < 0) {
                anniversary.setDaysText("已过" + Math.abs(days) + "天");
            } else if (days == 0) {
                anniversary.setDaysText("就是今天！");
            } else {
                anniversary.setDaysText("还有" + days + "天");
            }
        }
    }
    
    /**
     * 格式化天数文本
     */
    private String formatDaysText(long days) {
        if (days < 0) {
            return "还没开始";
        } else if (days == 0) {
            return "第1天";
        } else {
            return "第" + (days + 1) + "天";
        }
    }
}
