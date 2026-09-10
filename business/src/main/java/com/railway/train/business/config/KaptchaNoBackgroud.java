package com.railway.train.business.config;

import com.google.code.kaptcha.impl.DefaultKaptcha;
import com.google.code.kaptcha.util.Config;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import java.util.Properties;

/**
 * 无背景干扰的验证码，用于购买车票：
 * 样式简单清晰，方便用户在下单弹窗中快速识别输入。
 * <p>
 * 标注 @Primary：直接注入 DefaultKaptcha 时优先拿到这一个。
 */
@Configuration
public class KaptchaNoBackgroud {

    @Bean
    @Primary
    public DefaultKaptcha kaptchaNoBackgroud() {
        DefaultKaptcha defaultKaptcha = new DefaultKaptcha();
        Properties properties = new Properties();
        // 无边框
        properties.setProperty("kaptcha.border", "no");
        // 字体颜色
        properties.setProperty("kaptcha.textproducer.font.color", "blue");
        // 图片宽高
        properties.setProperty("kaptcha.image.width", "120");
        properties.setProperty("kaptcha.image.height", "40");
        // 字体大小
        properties.setProperty("kaptcha.textproducer.font.size", "32");
        // 字符间距
        properties.setProperty("kaptcha.textproducer.char.space", "3");
        Config config = new Config(properties);
        defaultKaptcha.setConfig(config);
        return defaultKaptcha;
    }
}
