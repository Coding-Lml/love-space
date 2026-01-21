package com.lovespace.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.lovespace.common.Result;
import com.lovespace.entity.Diary;
import com.lovespace.entity.User;
import com.lovespace.mapper.DiaryMapper;
import com.lovespace.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

@Service
@RequiredArgsConstructor
public class DiaryService extends ServiceImpl<DiaryMapper, Diary> {
    
    private final UserMapper userMapper;
    
    /**
     * 写日记
     */
    public Result<Diary> write(Long userId, Diary diary) {
        // 检查当天是否已写过日记
        Diary existDiary = this.getOne(new LambdaQueryWrapper<Diary>()
                .eq(Diary::getUserId, userId)
                .eq(Diary::getDiaryDate, diary.getDiaryDate()));
        
        if (existDiary != null) {
            // 更新已有日记
            existDiary.setTitle(diary.getTitle());
            existDiary.setContent(diary.getContent());
            existDiary.setMood(diary.getMood());
            existDiary.setWeather(diary.getWeather());
            existDiary.setVisibility(diary.getVisibility());
            this.updateById(existDiary);
            return Result.success("日记更新成功", existDiary);
        }
        
        // 新建日记
        diary.setUserId(userId);
        if (diary.getDiaryDate() == null) {
            diary.setDiaryDate(LocalDate.now());
        }
        if (diary.getVisibility() == null) {
            diary.setVisibility("both");
        }
        this.save(diary);
        
        return Result.success("日记保存成功", diary);
    }
    
    /**
     * 获取日记列表（分页）
     */
    public Result<Page<Diary>> getList(Long userId, Integer pageNum, Integer pageSize) {
        Page<Diary> page = new Page<>(pageNum, pageSize);
        
        // 查询自己的日记 + 对方可见的日记
        Page<Diary> diaryPage = this.page(page, new LambdaQueryWrapper<Diary>()
                .and(wrapper -> wrapper
                        .eq(Diary::getUserId, userId)
                        .or()
                        .eq(Diary::getVisibility, "both"))
                .orderByDesc(Diary::getDiaryDate));
        
        // 填充用户信息
        for (Diary diary : diaryPage.getRecords()) {
            fillDiaryUser(diary);
        }
        
        return Result.success(diaryPage);
    }
    
    /**
     * 获取某月的日记（日历视图用）
     */
    public Result<List<Diary>> getByMonth(Long userId, Integer year, Integer month) {
        YearMonth yearMonth = YearMonth.of(year, month);
        LocalDate startDate = yearMonth.atDay(1);
        LocalDate endDate = yearMonth.atEndOfMonth();
        
        List<Diary> diaries = this.list(new LambdaQueryWrapper<Diary>()
                .and(wrapper -> wrapper
                        .eq(Diary::getUserId, userId)
                        .or()
                        .eq(Diary::getVisibility, "both"))
                .between(Diary::getDiaryDate, startDate, endDate)
                .orderByAsc(Diary::getDiaryDate));
        
        for (Diary diary : diaries) {
            fillDiaryUser(diary);
        }
        
        return Result.success(diaries);
    }
    
    /**
     * 获取某天的日记
     */
    public Result<Diary> getByDate(Long userId, LocalDate date) {
        Diary diary = this.getOne(new LambdaQueryWrapper<Diary>()
                .and(wrapper -> wrapper
                        .eq(Diary::getUserId, userId)
                        .or()
                        .eq(Diary::getVisibility, "both"))
                .eq(Diary::getDiaryDate, date));
        
        if (diary != null) {
            fillDiaryUser(diary);
        }
        return Result.success(diary);
    }
    
    /**
     * 获取日记详情
     */
    public Result<Diary> getDetail(Long diaryId, Long userId) {
        Diary diary = this.getById(diaryId);
        if (diary == null) {
            return Result.error("日记不存在");
        }
        
        // 检查权限
        if (!diary.getUserId().equals(userId) && "self".equals(diary.getVisibility())) {
            return Result.error("无权查看此日记");
        }
        
        fillDiaryUser(diary);
        return Result.success(diary);
    }
    
    /**
     * 删除日记
     */
    public Result<Void> delete(Long diaryId, Long userId) {
        Diary diary = this.getById(diaryId);
        if (diary == null) {
            return Result.error("日记不存在");
        }
        if (!diary.getUserId().equals(userId)) {
            return Result.error("只能删除自己的日记");
        }
        
        this.removeById(diaryId);
        return Result.success("删除成功", null);
    }
    
    /**
     * 填充日记用户信息
     */
    private void fillDiaryUser(Diary diary) {
        User user = userMapper.selectById(diary.getUserId());
        if (user != null) {
            user.setPassword(null);
        }
        diary.setUser(user);
    }
}
