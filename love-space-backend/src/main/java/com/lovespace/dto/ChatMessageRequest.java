package com.lovespace.dto;

import lombok.Data;

@Data
public class ChatMessageRequest {

    private String type;

    private String content;

    private String mediaUrl;
}
