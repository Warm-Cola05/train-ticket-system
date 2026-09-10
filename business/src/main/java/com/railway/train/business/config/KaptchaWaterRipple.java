package com.railway.train.business.config;

import com.google.code.kaptcha.impl.DefaultKaptcha;
import com.google.code.kaptcha.util.Config;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Properties;

/**
 * 水波纹干扰样式的验证码，用于登录、注册等安全性要求较高的场景。
 * 按名称注入时使用：@Resource(name = "kaptchaWaterRipple")
 */
@Configuration
public class KaptchaWaterRipple {

    @Bean
    public DefaultKaptcha kaptchaWaterRipple() {
        DefaultKaptcha defaultKaptcha = new DefaultKaptcha();
        Properties properties = new Properties();
        // 无边框
        properties.setProperty("kaptcha.border", "no");
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
