package com.lovespace.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lovespace.common.Result;
import com.lovespace.entity.Diary;
import com.lovespace.service.DiaryService;
import com.lovespace.util.UserContext;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/diaries")
@RequiredArgsConstructor
@PreAuthorize("@roleService.isOwner()")
public class DiaryController {
    
    private final DiaryService diaryService;
    
    /**
     * 写日记
     */
    @PostMapping
    public Result<Diary> write(@RequestBody Diary diary) {
        Long userId = UserContext.getCurrentUserId();
        return diaryService.write(userId, diary);
    }
    
    /**
     * 获取日记列表
     */
    @GetMapping
    public Result<Page<Diary>> getList(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize) {
        Long userId = UserContext.getCurrentUserId();
        return diaryService.getList(userId, pageNum, pageSize);
    }
    
    /**
     * 获取某月日记（日历视图）
     */
    @GetMapping("/month")
    public Result<List<Diary>> getByMonth(
            @RequestParam Integer year,
            @RequestParam Integer month) {
        Long userId = UserContext.getCurrentUserId();
        return diaryService.getByMonth(userId, year, month);
    }
    
    /**
     * 获取某天的日记
     */
    @GetMapping("/date")
    public Result<Diary> getByDate(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        Long userId = UserContext.getCurrentUserId();
        return diaryService.getByDate(userId, date);
    }
    
    /**
     * 获取日记详情
     */
    @GetMapping("/{id}")
    public Result<Diary> getDetail(@PathVariable Long id) {
        Long userId = UserContext.getCurrentUserId();
        return diaryService.getDetail(id, userId);
    }
    
    /**
     * 更新日记
     */
    @PutMapping("/{id}")
    public Result<Diary> update(@PathVariable Long id, @RequestBody Diary diary) {
        Long userId = UserContext.getCurrentUserId();
        diary.setId(id);
        return diaryService.write(userId, diary);
    }
    
    /**
     * 删除日记
     */
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        Long userId = UserContext.getCurrentUserId();
        return diaryService.delete(id, userId);
    }
}
