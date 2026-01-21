package com.lovespace.controller;

import com.lovespace.common.Result;
import com.lovespace.service.FileService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/files")
@RequiredArgsConstructor
public class FileController {
    
    private final FileService fileService;
    
    /**
     * 上传单个文件
     */
    @PostMapping("/upload")
    public Result<String> upload(@RequestParam("file") MultipartFile file) {
        return fileService.uploadFile(file);
    }
    
    /**
     * 上传多个文件
     */
    @PostMapping("/upload/batch")
    public Result<List<String>> uploadBatch(@RequestParam("files") MultipartFile[] files) {
        return fileService.uploadFiles(files);
    }
    
    /**
     * 删除文件
     */
    @DeleteMapping
    public Result<Void> delete(@RequestParam("url") String url) {
        return fileService.deleteFile(url);
    }
}
