package com.lovespace.dto;

import com.lovespace.entity.User;
import lombok.Data;

@Data
public class GuestDashboardResponse {
    private DashboardData dashboard;
    private Couple couple;

    @Data
    public static class Couple {
        private User user1;
        private User user2;
    }
}

