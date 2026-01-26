package com.lovespace.service;

import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClientBuilder;
import com.lovespace.common.Result;
import com.lovespace.config.OssProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
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
    
    @Autowired(required = false)
    private OssProperties ossProperties;
    
    private static final List<String> IMAGE_TYPES = List.of("image/jpeg", "image/png", "image/gif", "image/webp");
    private static final List<String> VIDEO_TYPES = List.of("video/mp4", "video/quicktime", "video/x-msvideo", "video/webm");
    private static final List<String> AUDIO_TYPES = List.of(
            "audio/mpeg",
            "audio/mp3",
            "audio/aac",
            "audio/ogg",
            "audio/webm",
            "audio/wav",
            "audio/x-m4a"
    );

    private static final long IMAGE_OPTIMIZE_MIN_BYTES = 400 * 1024;
    private static final int IMAGE_OPTIMIZE_MAX_SIDE = 1600;
    
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
        } else if (AUDIO_TYPES.contains(contentType)) {
            subDir = "audios";
        } else {
            return Result.error("不支持的文件类型");
        }
        
        try {
            String url;
            if (isOssEnabled()) {
                url = uploadToOss(file, subDir);
            } else if ("images".equals(subDir) && shouldOptimizeImage(file, contentType)) {
                url = saveOptimizedImage(file, contentType);
            } else {
                url = saveFile(file, subDir);
            }
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
        String dateDir = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy/MM/dd"));
        String dirPath = uploadPath + File.separator + subDir + File.separator + dateDir;
        ensureDir(dirPath);
        
        String originalFilename = file.getOriginalFilename();
        String extension = "";
        if (originalFilename != null && originalFilename.contains(".")) {
            extension = originalFilename.substring(originalFilename.lastIndexOf("."));
        }
        String newFilename = UUID.randomUUID().toString().replace("-", "") + extension;
        
        String filePath = dirPath + File.separator + newFilename;
        file.transferTo(new File(filePath));
        
        return urlPrefix + "/" + subDir + "/" + dateDir + "/" + newFilename;
    }

    private boolean shouldOptimizeImage(MultipartFile file, String contentType) {
        if (file == null || contentType == null) {
            return false;
        }
        if (!("image/jpeg".equals(contentType) || "image/png".equals(contentType))) {
            return false;
        }
        if (file.getSize() < IMAGE_OPTIMIZE_MIN_BYTES) {
            return false;
        }
        String originalFilename = file.getOriginalFilename();
        if (originalFilename != null && originalFilename.toLowerCase().endsWith(".gif")) {
            return false;
        }
        return true;
    }

    private String saveOptimizedImage(MultipartFile file, String contentType) throws IOException {
        String dateDir = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy/MM/dd"));
        String dirPath = uploadPath + File.separator + "images" + File.separator + dateDir;
        ensureDir(dirPath);

        String extension = "image/png".equals(contentType) ? ".png" : ".jpg";
        String newFilename = UUID.randomUUID().toString().replace("-", "") + extension;
        File outFile = new File(dirPath + File.separator + newFilename);

        BufferedImage source;
        try (InputStream in = file.getInputStream()) {
            source = ImageIO.read(in);
        }
        if (source == null) {
            file.transferTo(outFile);
            return urlPrefix + "/images/" + dateDir + "/" + newFilename;
        }

        int sourceW = source.getWidth();
        int sourceH = source.getHeight();
        int maxSide = Math.max(sourceW, sourceH);
        double scale = maxSide > IMAGE_OPTIMIZE_MAX_SIDE ? (double) IMAGE_OPTIMIZE_MAX_SIDE / (double) maxSide : 1.0;
        int targetW = Math.max(1, (int) Math.round(sourceW * scale));
        int targetH = Math.max(1, (int) Math.round(sourceH * scale));

        boolean outputPng = "image/png".equals(contentType);
        BufferedImage target = new BufferedImage(
                targetW,
                targetH,
                outputPng ? BufferedImage.TYPE_INT_ARGB : BufferedImage.TYPE_INT_RGB
        );
        Graphics2D g2d = target.createGraphics();
        g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        if (!outputPng) {
            g2d.setColor(Color.WHITE);
            g2d.fillRect(0, 0, targetW, targetH);
        }
        g2d.drawImage(source, 0, 0, targetW, targetH, null);
        g2d.dispose();

        if (outputPng) {
            ImageIO.write(target, "png", outFile);
        } else {
            writeJpeg(target, outFile, 0.85f);
        }

        return urlPrefix + "/images/" + dateDir + "/" + newFilename;
    }

    private void writeJpeg(BufferedImage image, File outFile, float quality) throws IOException {
        ImageWriter writer = ImageIO.getImageWritersByFormatName("jpg").next();
        ImageWriteParam param = writer.getDefaultWriteParam();
        if (param.canWriteCompressed()) {
            param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
            param.setCompressionQuality(quality);
        }
        try (ImageOutputStream ios = ImageIO.createImageOutputStream(outFile)) {
            writer.setOutput(ios);
            writer.write(null, new IIOImage(image, null, null), param);
        } finally {
            writer.dispose();
        }
    }

    private void ensureDir(String dirPath) {
        File rootDir = new File(uploadPath);
        if (!rootDir.exists()) {
            rootDir.mkdirs();
        }
        File dir = new File(dirPath);
        if (!dir.exists()) {
            dir.mkdirs();
        }
    }
    
    /**
     * 删除文件
     */
    public Result<Void> deleteFile(String url) {
        if (url == null) {
            return Result.error("无效的文件URL");
        }
        if (isOssEnabled() && isOssUrl(url)) {
            try {
                deleteFromOss(url);
            } catch (Exception e) {
                log.error("删除OSS文件失败", e);
            }
            return Result.success("删除成功", null);
        }
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
        } else if (AUDIO_TYPES.contains(contentType)) {
            return "audio";
        }
        return "unknown";
    }

    public String buildThumbnailUrl(String url) {
        if (url == null || url.isBlank()) {
            return url;
        }
        if (url.contains("x-oss-process=")) {
            return url;
        }
        String hash = "";
        int hashIndex = url.indexOf('#');
        if (hashIndex >= 0) {
            hash = url.substring(hashIndex);
            url = url.substring(0, hashIndex);
        }
        String joiner = url.contains("?") ? "&" : "?";
        return url + joiner + "x-oss-process=image/resize,w_480/quality,q_80" + hash;
    }
    
    private boolean isOssEnabled() {
        if (ossProperties == null) {
            return false;
        }
        if (!ossProperties.isEnabled()) {
            return false;
        }
        if (ossProperties.getEndpoint() == null || ossProperties.getEndpoint().isBlank()) {
            return false;
        }
        if (ossProperties.getAccessKeyId() == null || ossProperties.getAccessKeyId().isBlank()) {
            return false;
        }
        if (ossProperties.getAccessKeySecret() == null || ossProperties.getAccessKeySecret().isBlank()) {
            return false;
        }
        if (ossProperties.getBucket() == null || ossProperties.getBucket().isBlank()) {
            return false;
        }
        return true;
    }
    
    private String uploadToOss(MultipartFile file, String subDir) throws IOException {
        String dateDir = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy/MM/dd"));
        String basePath = ossProperties.getBasePath();
        if (basePath == null) {
            basePath = "";
        } else {
            basePath = basePath.trim();
        }
        String originalFilename = file.getOriginalFilename();
        String extension = "";
        if (originalFilename != null && originalFilename.contains(".")) {
            extension = originalFilename.substring(originalFilename.lastIndexOf("."));
        }
        String newFilename = UUID.randomUUID().toString().replace("-", "") + extension;
        StringBuilder keyBuilder = new StringBuilder();
        if (!basePath.isEmpty()) {
            keyBuilder.append(basePath);
            if (!basePath.endsWith("/")) {
                keyBuilder.append("/");
            }
        }
        keyBuilder.append(subDir).append("/").append(dateDir).append("/").append(newFilename);
        String objectKey = keyBuilder.toString();
        OSS client = new OSSClientBuilder().build(ossProperties.getEndpoint(), ossProperties.getAccessKeyId(), ossProperties.getAccessKeySecret());
        try (InputStream in = file.getInputStream()) {
            client.putObject(ossProperties.getBucket(), objectKey, in);
        } finally {
            client.shutdown();
        }
        String domain = ossProperties.getDomain();
        String baseUrl;
        if (domain != null && !domain.isBlank()) {
            baseUrl = domain;
        } else {
            baseUrl = "https://" + ossProperties.getBucket() + "." + ossProperties.getEndpoint();
        }
        while (baseUrl.endsWith("/")) {
            baseUrl = baseUrl.substring(0, baseUrl.length() - 1);
        }
        return baseUrl + "/" + objectKey;
    }
    
    private boolean isOssUrl(String url) {
        if (ossProperties == null) {
            return false;
        }
        String domain = ossProperties.getDomain();
        if (domain != null && !domain.isBlank() && url.startsWith(domain)) {
            return true;
        }
        String endpoint = ossProperties.getEndpoint();
        String bucket = ossProperties.getBucket();
        if (endpoint == null || endpoint.isBlank() || bucket == null || bucket.isBlank()) {
            return false;
        }
        String prefix = "https://" + bucket + "." + endpoint;
        return url.startsWith(prefix);
    }
    
    private void deleteFromOss(String url) {
        String domain = ossProperties.getDomain();
        String endpoint = ossProperties.getEndpoint();
        String bucket = ossProperties.getBucket();
        String prefix;
        if (domain != null && !domain.isBlank() && url.startsWith(domain)) {
            prefix = domain;
        } else {
            prefix = "https://" + bucket + "." + endpoint;
        }
        String key = url.substring(prefix.length());
        int queryIndex = key.indexOf("?");
        if (queryIndex >= 0) {
            key = key.substring(0, queryIndex);
        }
        int fragmentIndex = key.indexOf("#");
        if (fragmentIndex >= 0) {
            key = key.substring(0, fragmentIndex);
        }
        while (key.startsWith("/")) {
            key = key.substring(1);
        }
        if (key.isEmpty()) {
            return;
        }
        OSS client = new OSSClientBuilder().build(endpoint, ossProperties.getAccessKeyId(), ossProperties.getAccessKeySecret());
        try {
            client.deleteObject(bucket, key);
        } finally {
            client.shutdown();
        }
    }
}
