package com.lovespace.config;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.lovespace.entity.User;
import com.lovespace.mapper.UserMapper;
import com.lovespace.util.PasswordUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {
    
    private final UserMapper userMapper;
    private final PasswordUtil passwordUtil;
    
    @Override
    public void run(String... args) {
        // 检查是否需要初始化用户密码
        User user = userMapper.selectOne(new LambdaQueryWrapper<User>()
                .eq(User::getUsername, "limenglong"));
        
        if (user != null && user.getPassword().startsWith("$2a$")) {
            // 密码已经是 BCrypt 格式，无需处理
            log.info("用户密码已初始化");
            return;
        }
        
        // 更新所有用户密码为 BCrypt 格式
        // 默认密码: love520
        String encodedPassword = passwordUtil.encode("love520");
        
        userMapper.selectList(null).forEach(u -> {
            u.setPassword(encodedPassword);
            userMapper.updateById(u);
            log.info("用户 {} 密码已更新", u.getUsername());
        });
        
        log.info("=================================");
        log.info("用户密码初始化完成！");
        log.info("默认密码: love520");
        log.info("请登录后及时修改密码");
        log.info("=================================");
    }
}
