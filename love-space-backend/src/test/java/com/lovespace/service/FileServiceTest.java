package com.lovespace.service;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class FileServiceTest {

    private final FileService fileService = new FileService();

    @Test
    void getFileTypeAcceptsAudioMimeWithCodecParameters() {
        assertThat(fileService.getFileType("audio/webm;codecs=opus")).isEqualTo("audio");
    }
}
