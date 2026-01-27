package com.lovespace.controller;

import com.lovespace.common.Result;
import com.lovespace.dto.SpaceDetail;
import com.lovespace.service.SpaceService;
import com.lovespace.util.UserContext;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/spaces")
@RequiredArgsConstructor
@PreAuthorize("@roleService.isOwner()")
public class SpaceController {

    private final SpaceService spaceService;

    @GetMapping("/current")
    public Result<SpaceDetail> getCurrent() {
        Long userId = UserContext.getCurrentUserId();
        SpaceDetail detail = spaceService.getCurrentSpaceDetail(userId);
        return Result.success(detail);
    }
}
