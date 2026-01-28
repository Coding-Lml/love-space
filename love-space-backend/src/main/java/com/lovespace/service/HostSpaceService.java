package com.lovespace.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.lovespace.entity.User;
import com.lovespace.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class HostSpaceService {

    @Value("${couple.user1.username}")
    private String ownerUsername1;

    @Value("${couple.user2.username}")
    private String ownerUsername2;

    private final UserMapper userMapper;
    private final SpaceService spaceService;

    private volatile Long hostUserId;
    private volatile Long hostSpaceId;

    public Long getHostUserId() {
        Long cached = hostUserId;
        if (cached != null) {
            return cached;
        }
        synchronized (this) {
            if (hostUserId != null) {
                return hostUserId;
            }
            User host = userMapper.selectOne(new LambdaQueryWrapper<User>().eq(User::getUsername, ownerUsername1));
            if (host == null) {
                host = userMapper.selectOne(new LambdaQueryWrapper<User>().eq(User::getUsername, ownerUsername2));
            }
            if (host == null) {
                host = userMapper.selectOne(new LambdaQueryWrapper<User>()
                        .orderByAsc(User::getId)
                        .last("LIMIT 1"));
            }
            hostUserId = host == null ? null : host.getId();
            return hostUserId;
        }
    }

    public Long getHostSpaceId() {
        Long cached = hostSpaceId;
        if (cached != null) {
            return cached;
        }
        synchronized (this) {
            if (hostSpaceId != null) {
                return hostSpaceId;
            }
            Long userId = getHostUserId();
            if (userId == null) {
                return null;
            }
            hostSpaceId = spaceService.getOrCreatePrimarySpaceId(userId);
            return hostSpaceId;
        }
    }
}
