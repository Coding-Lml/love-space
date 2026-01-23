package com.lovespace.service;

import com.lovespace.common.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
public class FileService {
    
    @Value("${upload.path}")
    private String uploadPath;
    
    @Value("${upload.url-prefix}")
    private String urlPrefix;
    
    // 允许的图片类型
    private static final List<String> IMAGE_TYPES = List.of("image/jpeg", "image/png", "image/gif", "image/webp");
    // 允许的视频类型
    private static final List<String> VIDEO_TYPES = List.of("video/mp4", "video/quicktime", "video/x-msvideo", "video/webm");
    
    /**
     * 上传单个文件
     */
    public Result<String> uploadFile(MultipartFile file) {
        if (file.isEmpty()) {
            return Result.error("文件不能为空");
        }
        
        String contentType = file.getContentType();
        String subDir;
        
        if (IMAGE_TYPES.contains(contentType)) {
            subDir = "images";
        } else if (VIDEO_TYPES.contains(contentType)) {
            subDir = "videos";
        } else {
            return Result.error("不支持的文件类型");
        }
        
        try {
            String url = saveFile(file, subDir);
            return Result.success("上传成功", url);
        } catch (IOException e) {
            log.error("文件上传失败", e);
            return Result.error("文件上传失败：" + e.getMessage());
        }
    }
    
    /**
     * 上传多个文件
     */
    public Result<List<String>> uploadFiles(MultipartFile[] files) {
        if (files == null || files.length == 0) {
            return Result.error("文件不能为空");
        }
        
        List<String> urls = new ArrayList<>();
        for (MultipartFile file : files) {
            Result<String> result = uploadFile(file);
            if (result.getCode() != 200) {
                return Result.error(result.getMessage());
            }
            urls.add(result.getData());
        }
        
        return Result.success("上传成功", urls);
    }
    
    /**
     * 保存文件
     */
    private String saveFile(MultipartFile file, String subDir) throws IOException {
        // 生成日期目录
        String dateDir = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy/MM/dd"));
        
        // 使用系统文件分隔符构建路径
        String dirPath = uploadPath + File.separator + subDir + File.separator + dateDir;
        File dir = new File(dirPath);
        
        // 确保上传根目录存在
        File rootDir = new File(uploadPath);
        if (!rootDir.exists()) {
            rootDir.mkdirs();
        }
        
        // 确保子目录存在
        if (!dir.exists()) {
            dir.mkdirs();
        }
        
        // 生成文件名
        String originalFilename = file.getOriginalFilename();
        String extension = "";
        if (originalFilename != null && originalFilename.contains(".")) {
            extension = originalFilename.substring(originalFilename.lastIndexOf("."));
        }
        String newFilename = UUID.randomUUID().toString().replace("-", "") + extension;
        
        // 保存文件
        String filePath = dirPath + File.separator + newFilename;
        file.transferTo(new File(filePath));
        
        // 返回访问URL (使用统一的URL分隔符)
        return urlPrefix + "/" + subDir + "/" + dateDir + "/" + newFilename;
    }
    
    /**
     * 删除文件
     */
    public Result<Void> deleteFile(String url) {
        if (url == null || !url.startsWith(urlPrefix)) {
            return Result.error("无效的文件URL");
        }

        String relativePath = url.substring(urlPrefix.length());
        int queryIndex = relativePath.indexOf('?');
        if (queryIndex >= 0) {
            relativePath = relativePath.substring(0, queryIndex);
        }
        int fragmentIndex = relativePath.indexOf('#');
        if (fragmentIndex >= 0) {
            relativePath = relativePath.substring(0, fragmentIndex);
        }

        while (relativePath.startsWith("/")) {
            relativePath = relativePath.substring(1);
        }

        Path baseDir = Paths.get(uploadPath).toAbsolutePath().normalize();
        Path targetPath = baseDir.resolve(relativePath.replace("/", File.separator)).normalize();

        if (!targetPath.startsWith(baseDir)) {
            return Result.error("无效的文件URL");
        }

        File file = targetPath.toFile();
        if (file.exists()) {
            if (file.isDirectory()) {
                return Result.error("无效的文件URL");
            }
            if (file.delete()) {
                return Result.success("删除成功", null);
            } else {
                return Result.error("删除失败");
            }
        }
        
        return Result.success("文件不存在", null);
    }
    
    /**
     * 获取文件类型
     */
    public String getFileType(String contentType) {
        if (IMAGE_TYPES.contains(contentType)) {
            return "image";
        } else if (VIDEO_TYPES.contains(contentType)) {
            return "video";
        }
        return "unknown";
    }
}
