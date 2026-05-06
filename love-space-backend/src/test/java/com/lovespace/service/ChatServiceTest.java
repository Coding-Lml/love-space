package com.lovespace.service;

import com.lovespace.common.Result;
import com.lovespace.entity.ChatMessage;
import com.lovespace.mapper.ChatMessageMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ChatServiceTest {

    @Mock
    private ChatMessageMapper chatMessageMapper;

    @Mock
    private SpaceService spaceService;

    private ChatService chatService;

    @BeforeEach
    void setUp() {
        chatService = new ChatService(chatMessageMapper, spaceService);
    }

    @Test
    void sendTextMessageTrimsContentAndTargetsPartner() {
        when(spaceService.getOrCreatePrimarySpaceId(1L)).thenReturn(10L);
        when(spaceService.getPartnerUserIdInPrimarySpace(1L)).thenReturn(2L);
        when(chatMessageMapper.insert(any(ChatMessage.class))).thenAnswer(invocation -> {
            ChatMessage message = invocation.getArgument(0);
            message.setId(99L);
            return 1;
        });

        Result<ChatMessage> result = chatService.sendTextOrSticker(1L, "text", "  想你啦  ", null);

        assertThat(result.getCode()).isEqualTo(200);
        assertThat(result.getData().getId()).isEqualTo(99L);

        ArgumentCaptor<ChatMessage> captor = ArgumentCaptor.forClass(ChatMessage.class);
        org.mockito.Mockito.verify(chatMessageMapper).insert(captor.capture());
        ChatMessage saved = captor.getValue();
        assertThat(saved.getSpaceId()).isEqualTo(10L);
        assertThat(saved.getFromUserId()).isEqualTo(1L);
        assertThat(saved.getToUserId()).isEqualTo(2L);
        assertThat(saved.getType()).isEqualTo("text");
        assertThat(saved.getContent()).isEqualTo("想你啦");
        assertThat(saved.getMediaUrl()).isNull();
        assertThat(saved.getStatus()).isEqualTo("sent");
    }

    @Test
    void sendStickerMessageAllowsMediaUrlWithoutText() {
        when(spaceService.getOrCreatePrimarySpaceId(1L)).thenReturn(10L);
        when(spaceService.getPartnerUserIdInPrimarySpace(1L)).thenReturn(2L);
        when(chatMessageMapper.insert(any(ChatMessage.class))).thenReturn(1);

        Result<ChatMessage> result = chatService.sendTextOrSticker(1L, "sticker", null, "/stickers/love-1.gif");

        assertThat(result.getCode()).isEqualTo(200);
        ArgumentCaptor<ChatMessage> captor = ArgumentCaptor.forClass(ChatMessage.class);
        org.mockito.Mockito.verify(chatMessageMapper).insert(captor.capture());
        assertThat(captor.getValue().getType()).isEqualTo("sticker");
        assertThat(captor.getValue().getMediaUrl()).isEqualTo("/stickers/love-1.gif");
    }

    @Test
    void sendMediaMessageStoresUrlAndExtra() {
        when(spaceService.getOrCreatePrimarySpaceId(1L)).thenReturn(10L);
        when(spaceService.getPartnerUserIdInPrimarySpace(1L)).thenReturn(2L);
        when(chatMessageMapper.insert(any(ChatMessage.class))).thenReturn(1);

        Result<ChatMessage> result = chatService.sendMedia(1L, "audio", "/uploads/audios/today/a.webm", null, "{\"duration\":8}");

        assertThat(result.getCode()).isEqualTo(200);
        ArgumentCaptor<ChatMessage> captor = ArgumentCaptor.forClass(ChatMessage.class);
        org.mockito.Mockito.verify(chatMessageMapper).insert(captor.capture());
        assertThat(captor.getValue().getType()).isEqualTo("audio");
        assertThat(captor.getValue().getMediaUrl()).isEqualTo("/uploads/audios/today/a.webm");
        assertThat(captor.getValue().getExtra()).isEqualTo("{\"duration\":8}");
    }

    @Test
    void sendTextMessageRejectsUserWithoutPartnerInPrimarySpace() {
        when(spaceService.getOrCreatePrimarySpaceId(9L)).thenReturn(30L);
        when(spaceService.getPartnerUserIdInPrimarySpace(9L)).thenReturn(null);

        Result<ChatMessage> result = chatService.sendTextOrSticker(9L, "text", "hello", null);

        assertThat(result.getCode()).isEqualTo(403);
        assertThat(result.getMessage()).isEqualTo("当前空间没有可聊天的另一半");
    }

    @Test
    void markReadUpdatesOnlyMessagesReceivedByCurrentUser() {
        when(spaceService.getOrCreatePrimarySpaceId(1L)).thenReturn(10L);
        when(spaceService.getPartnerUserIdInPrimarySpace(1L)).thenReturn(2L);
        when(chatMessageMapper.update(any(ChatMessage.class), any())).thenReturn(3);

        Result<Integer> result = chatService.markRead(1L);

        assertThat(result.getCode()).isEqualTo(200);
        assertThat(result.getData()).isEqualTo(3);
        ArgumentCaptor<ChatMessage> captor = ArgumentCaptor.forClass(ChatMessage.class);
        org.mockito.Mockito.verify(chatMessageMapper).update(captor.capture(), any());
        assertThat(captor.getValue().getStatus()).isEqualTo("read");
    }

    @Test
    void unreadCountReturnsMapperCount() {
        when(spaceService.getOrCreatePrimarySpaceId(1L)).thenReturn(10L);
        when(spaceService.getPartnerUserIdInPrimarySpace(1L)).thenReturn(2L);
        when(chatMessageMapper.selectCount(any())).thenReturn(5L);

        Result<Long> result = chatService.getUnreadCount(1L);

        assertThat(result.getCode()).isEqualTo(200);
        assertThat(result.getData()).isEqualTo(5L);
    }
}
