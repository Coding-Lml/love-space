package com.lovespace.controller;

import com.lovespace.common.Result;
import com.lovespace.entity.Anniversary;
import com.lovespace.service.AnniversaryService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/anniversaries")
@RequiredArgsConstructor
public class AnniversaryController {
    
    private final AnniversaryService anniversaryService;
    
    /**
     * 添加纪念日
     */
    @PostMapping
    public Result<Anniversary> add(@RequestBody Anniversary anniversary) {
        return anniversaryService.add(anniversary);
    }
    
    /**
     * 获取所有纪念日
     */
    @GetMapping
    public Result<List<Anniversary>> getAll() {
        return anniversaryService.getAll();
    }
    
    /**
     * 获取在一起天数
     */
    @GetMapping("/together")
    public Result<Anniversary> getTogetherDays() {
        return anniversaryService.getTogetherDays();
    }
    
    /**
     * 获取即将到来的纪念日
     */
    @GetMapping("/upcoming")
    public Result<List<Anniversary>> getUpcoming(@RequestParam(defaultValue = "30") Integer days) {
        List<Anniversary> list = anniversaryService.getUpcoming(days);
        return Result.success(list);
    }
    
    /**
     * 更新纪念日
     */
    @PutMapping("/{id}")
    public Result<Anniversary> update(@PathVariable Long id, @RequestBody Anniversary anniversary) {
        anniversary.setId(id);
        return anniversaryService.update(anniversary);
    }
    
    /**
     * 删除纪念日
     */
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        return anniversaryService.delete(id);
    }
}
