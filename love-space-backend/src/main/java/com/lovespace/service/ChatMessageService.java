package com.lovespace.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.lovespace.entity.ChatMessage;
import com.lovespace.mapper.ChatMessageMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ChatMessageService extends ServiceImpl<ChatMessageMapper, ChatMessage> {

    private final SpaceService spaceService;

    public Long getPartnerId(Long userId) {
        return spaceService.getPartnerUserIdInPrimarySpace(userId);
    }

    public List<ChatMessage> getHistory(Long userId, Long partnerId, Long beforeId, int size) {
        Long spaceId = spaceService.getOrCreatePrimarySpaceId(userId);
        LambdaQueryWrapper<ChatMessage> wrapper = new LambdaQueryWrapper<ChatMessage>()
                .eq(ChatMessage::getSpaceId, spaceId)
                .and(w -> w.eq(ChatMessage::getFromUserId, userId)
                        .eq(ChatMessage::getToUserId, partnerId)
                        .or()
                        .eq(ChatMessage::getFromUserId, partnerId)
                        .eq(ChatMessage::getToUserId, userId))
                .orderByDesc(ChatMessage::getId);

        if (beforeId != null && beforeId > 0) {
            wrapper.lt(ChatMessage::getId, beforeId);
        }

        wrapper.last("LIMIT " + size);
        List<ChatMessage> list = this.list(wrapper);
        list.sort((a, b) -> Long.compare(a.getId(), b.getId()));
        return list;
    }

    public List<Long> markAllRead(Long userId, Long partnerId) {
        Long spaceId = spaceService.getOrCreatePrimarySpaceId(userId);
        LambdaQueryWrapper<ChatMessage> wrapper = new LambdaQueryWrapper<ChatMessage>()
                .eq(ChatMessage::getSpaceId, spaceId)
                .eq(ChatMessage::getToUserId, userId)
                .eq(ChatMessage::getFromUserId, partnerId)
                .eq(ChatMessage::getStatus, "sent");
        List<ChatMessage> list = this.list(wrapper);
        if (list.isEmpty()) {
            return List.of();
        }
        ChatMessage update = new ChatMessage();
        update.setStatus("read");
        this.update(update, wrapper);
        return list.stream().map(ChatMessage::getId).toList();
    }
}
