package com.railway.train.business.config;

import com.google.code.kaptcha.impl.DefaultKaptcha;
import com.google.code.kaptcha.util.Config;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Properties;

/**
 * 图片验证码配置（默认样式）
 * <p>
 * kaptcha 生成的验证码是纯 AWT 图片，与 Servlet 无关，
 * 所以在 Spring Boot 3（jakarta）下可以直接使用。
 */
@Configuration
public class KaptchaConfig {

    /**
     * 默认样式：水波纹干扰 + 噪点背景，适合登录、注册等安全性要求较高的场景
     */
    @Bean
    public DefaultKaptcha getDefaultKaptcha() {
        DefaultKaptcha defaultKaptcha = new DefaultKaptcha();
        Properties properties = new Properties();
        // 是否有边框
        properties.setProperty("kaptcha.border", "yes");
        // 边框颜色
        properties.setProperty("kaptcha.border.color", "105,179,90");
        // 图片宽高
        properties.setProperty("kaptcha.image.width", "120");
        properties.setProperty("kaptcha.image.height", "40");
        // 字体大小与字体
        properties.setProperty("kaptcha.textproducer.font.size", "32");
        properties.setProperty("kaptcha.textproducer.font.names", "Arial");
        // 验证码长度
        properties.setProperty("kaptcha.textproducer.char.length", "4");
        // 干扰实现类：水波纹
        properties.setProperty("kaptcha.noise.impl", "com.google.code.kaptcha.impl.WaterRipple");
        Config config = new Config(properties);
        defaultKaptcha.setConfig(config);
        return defaultKaptcha;
    }
}
