package com.lovespace;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("com.lovespace.mapper")
public class LoveSpaceApplication {

    public static void main(String[] args) {
        SpringApplication.run(LoveSpaceApplication.class, args);
        System.out.println("\n" +
            "  ╔═══════════════════════════════════════════╗\n" +
            "  ║                                           ║\n" +
            "  ║   💕 Love Space 情侣空间启动成功！💕      ║\n" +
            "  ║                                           ║\n" +
            "  ║   李梦龙 ❤️ 曾凡芮                         ║\n" +
            "  ║                                           ║\n" +
            "  ║   访问地址: http://localhost:8080         ║\n" +
            "  ║                                           ║\n" +
            "  ╚═══════════════════════════════════════════╝\n");
    }
}
