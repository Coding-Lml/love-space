package com.lovespace.controller;

import com.lovespace.common.Result;
import com.lovespace.entity.Anniversary;
import com.lovespace.service.AnniversaryService;
import com.lovespace.util.UserContext;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/anniversaries")
@RequiredArgsConstructor
@PreAuthorize("@roleService.isOwner()")
public class AnniversaryController {
    
    private final AnniversaryService anniversaryService;
    
    /**
     * 添加纪念日
     */
    @PostMapping
    public Result<Anniversary> add(@RequestBody Anniversary anniversary) {
        Long userId = UserContext.getCurrentUserId();
        return anniversaryService.add(userId, anniversary);
    }
    
    /**
     * 获取所有纪念日
     */
    @GetMapping
    public Result<List<Anniversary>> getAll() {
        Long userId = UserContext.getCurrentUserId();
        return anniversaryService.getAll(userId);
    }
    
    /**
     * 获取在一起天数
     */
    @GetMapping("/together")
    public Result<Anniversary> getTogetherDays() {
        Long userId = UserContext.getCurrentUserId();
        return anniversaryService.getTogetherDays(userId);
    }
    
    /**
     * 获取即将到来的纪念日
     */
    @GetMapping("/upcoming")
    public Result<List<Anniversary>> getUpcoming(@RequestParam(defaultValue = "30") Integer days) {
        Long userId = UserContext.getCurrentUserId();
        List<Anniversary> list = anniversaryService.getUpcoming(userId, days);
        return Result.success(list);
    }
    
    /**
     * 更新纪念日
     */
    @PutMapping("/{id}")
    public Result<Anniversary> update(@PathVariable Long id, @RequestBody Anniversary anniversary) {
        Long userId = UserContext.getCurrentUserId();
        anniversary.setId(id);
        return anniversaryService.update(userId, anniversary);
    }
    
    /**
     * 删除纪念日
     */
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        Long userId = UserContext.getCurrentUserId();
        return anniversaryService.delete(userId, id);
    }
}
