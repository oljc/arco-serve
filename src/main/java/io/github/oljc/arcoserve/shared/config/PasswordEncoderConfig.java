package io.github.oljc.arcoserve.shared.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * 密码加密器配置
 * 将密码加密器配置从SecurityConfig中分离出来，避免循环依赖
 */
@Configuration
public class PasswordEncoderConfig {

    /**
     * 密码加密器
     * 使用BCrypt加密算法，强度为12
     *
     * @return 密码加密器
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }
}