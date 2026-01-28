package com.lovespace.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.lovespace.dto.SpaceDetail;
import com.lovespace.entity.Space;
import com.lovespace.entity.SpaceMember;
import com.lovespace.entity.User;
import com.lovespace.mapper.SpaceMapper;
import com.lovespace.mapper.SpaceMemberMapper;
import com.lovespace.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SpaceService {

    @Value("${couple.space-name:我们的空间}")
    private String defaultSpaceName;

    @Value("${couple.user1.username:limenglong}")
    private String ownerUsername1;

    @Value("${couple.user2.username:zengfanrui}")
    private String ownerUsername2;

    private final SpaceMapper spaceMapper;
    private final SpaceMemberMapper spaceMemberMapper;
    private final UserMapper userMapper;

    @Transactional
    public Long getOrCreatePrimarySpaceId(Long userId) {
        if (userId == null) {
            return null;
        }

        SpaceMember existing = spaceMemberMapper.selectOne(new LambdaQueryWrapper<SpaceMember>()
                .eq(SpaceMember::getUserId, userId)
                .orderByAsc(SpaceMember::getId)
                .last("LIMIT 1"));
        if (existing != null) {
            return existing.getSpaceId();
        }

        User current = userMapper.selectById(userId);
        String username = current == null ? null : current.getUsername();

        if (ownerUsername1.equals(username) || ownerUsername2.equals(username)) {
            Space space = spaceMapper.selectOne(new LambdaQueryWrapper<Space>()
                    .eq(Space::getName, defaultSpaceName)
                    .orderByAsc(Space::getId)
                    .last("LIMIT 1"));
            if (space == null) {
                space = new Space();
                space.setName(defaultSpaceName);
                spaceMapper.insert(space);
            }
            ensureDefaultCoupleMembers(space.getId());
            return space.getId();
        }

        Space space = new Space();
        space.setName((current != null && current.getNickname() != null && !current.getNickname().isBlank())
                ? current.getNickname() + "的空间"
                : "我的空间");
        spaceMapper.insert(space);
        SpaceMember member = new SpaceMember();
        member.setSpaceId(space.getId());
        member.setUserId(userId);
        member.setRole("OWNER");
        spaceMemberMapper.insert(member);
        return space.getId();
    }

    public Long getPartnerUserIdInPrimarySpace(Long userId) {
        Long spaceId = getOrCreatePrimarySpaceId(userId);
        if (spaceId == null) {
            return null;
        }
        List<SpaceMember> members = spaceMemberMapper.selectList(new LambdaQueryWrapper<SpaceMember>()
                .eq(SpaceMember::getSpaceId, spaceId));
        if (members == null || members.isEmpty()) {
            return null;
        }
        for (SpaceMember m : members) {
            if (m.getUserId() != null && !m.getUserId().equals(userId)) {
                return m.getUserId();
            }
        }
        return null;
    }

    public SpaceDetail getCurrentSpaceDetail(Long userId) {
        Long spaceId = getOrCreatePrimarySpaceId(userId);
        if (spaceId == null) {
            return null;
        }
        Space space = spaceMapper.selectById(spaceId);
        List<SpaceMember> members = spaceMemberMapper.selectList(new LambdaQueryWrapper<SpaceMember>()
                .eq(SpaceMember::getSpaceId, spaceId));

        Map<Long, SpaceMember> memberByUserId = members.stream()
                .filter(m -> m.getUserId() != null)
                .collect(Collectors.toMap(SpaceMember::getUserId, m -> m, (a, b) -> a));

        List<Long> userIds = members.stream()
                .map(SpaceMember::getUserId)
                .filter(Objects::nonNull)
                .toList();

        List<User> users = userIds.isEmpty() ? List.of() : userMapper.selectBatchIds(userIds);
        for (User u : users) {
            if (u != null) {
                u.setPassword(null);
            }
        }

        users = users.stream()
                .filter(Objects::nonNull)
                .sorted((a, b) -> {
                    SpaceMember ma = memberByUserId.get(a.getId());
                    SpaceMember mb = memberByUserId.get(b.getId());
                    int ra = ma != null && "OWNER".equals(ma.getRole()) ? 0 : 1;
                    int rb = mb != null && "OWNER".equals(mb.getRole()) ? 0 : 1;
                    if (ra != rb) {
                        return Integer.compare(ra, rb);
                    }
                    if (ma != null && mb != null && ma.getJoinedAt() != null && mb.getJoinedAt() != null) {
                        return ma.getJoinedAt().compareTo(mb.getJoinedAt());
                    }
                    return Long.compare(a.getId(), b.getId());
                })
                .toList();

        SpaceDetail detail = new SpaceDetail();
        detail.setSpace(space);
        detail.setMembers(users);
        return detail;
    }

    private void ensureDefaultCoupleMembers(Long spaceId) {
        if (spaceId == null) {
            return;
        }
        ensureMember(spaceId, ownerUsername1, "OWNER");
        ensureMember(spaceId, ownerUsername2, "MEMBER");
    }

    private void ensureMember(Long spaceId, String username, String role) {
        User user = userMapper.selectOne(new LambdaQueryWrapper<User>()
                .eq(User::getUsername, username)
                .last("LIMIT 1"));
        if (user == null) {
            return;
        }
        SpaceMember exist = spaceMemberMapper.selectOne(new LambdaQueryWrapper<SpaceMember>()
                .eq(SpaceMember::getSpaceId, spaceId)
                .eq(SpaceMember::getUserId, user.getId())
                .last("LIMIT 1"));
        if (exist != null) {
            if (exist.getRole() == null || exist.getRole().isBlank() || !exist.getRole().equals(role)) {
                exist.setRole(role);
                spaceMemberMapper.updateById(exist);
            }
            return;
        }
        SpaceMember member = new SpaceMember();
        member.setSpaceId(spaceId);
        member.setUserId(user.getId());
        member.setRole(role);
        spaceMemberMapper.insert(member);
    }
}
