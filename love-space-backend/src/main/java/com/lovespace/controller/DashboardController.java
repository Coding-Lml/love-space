package com.lovespace.controller;

import com.lovespace.common.Result;
import com.lovespace.dto.DashboardData;
import com.lovespace.service.DashboardService;
import com.lovespace.util.UserContext;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
public class DashboardController {
    
    private final DashboardService dashboardService;
    
    /**
     * 获取仪表盘数据
     */
    @GetMapping
    public Result<DashboardData> getDashboard() {
        Long userId = UserContext.getCurrentUserId();
        DashboardData data = dashboardService.getDashboardData(userId);
        return Result.success(data);
    }
}
