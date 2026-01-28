package com.lovespace.config;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.lovespace.entity.User;
import com.lovespace.mapper.UserMapper;
import com.lovespace.util.PasswordUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Slf4j
@Component
@Profile("dev")
@ConditionalOnProperty(prefix = "app.dev.seed", name = "enabled", havingValue = "true")
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {
    
    private final UserMapper userMapper;
    private final PasswordUtil passwordUtil;

    @Value("${app.dev.seed.password:love520}")
    private String seedPassword;

    @Value("${couple.user1.username:}")
    private String ownerUsername1;

    @Value("${couple.user1.nickname:}")
    private String ownerNickname1;

    @Value("${couple.user2.username:}")
    private String ownerUsername2;

    @Value("${couple.user2.nickname:}")
    private String ownerNickname2;
    
    @Override
    public void run(String... args) {
        createUserIfAbsent(ownerUsername1, ownerNickname1);
        createUserIfAbsent(ownerUsername2, ownerNickname2);
    }

    private void createUserIfAbsent(String username, String nickname) {
        if (!StringUtils.hasText(username)) {
            return;
        }
        String normalizedUsername = username.trim();

        User exist = userMapper.selectOne(new LambdaQueryWrapper<User>()
                .eq(User::getUsername, normalizedUsername)
                .last("LIMIT 1"));
        if (exist != null) {
            return;
        }

        User user = new User();
        user.setUsername(normalizedUsername);
        user.setPassword(passwordUtil.encode(seedPassword));
        user.setNickname(StringUtils.hasText(nickname) ? nickname.trim() : normalizedUsername);
        user.setAvatar("/uploads/images/default-avatar-boy.png");
        userMapper.insert(user);
        log.info("已创建示例用户：{}", normalizedUsername);
    }
}
