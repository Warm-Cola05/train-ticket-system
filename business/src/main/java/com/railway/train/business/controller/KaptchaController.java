package com.railway.train.business.controller;

import com.google.code.kaptcha.impl.DefaultKaptcha;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.util.concurrent.TimeUnit;

/**
 * 图片验证码接口：用于购买车票（下单）时的安全校验。
 * <p>
 * 流程：
 * 1. 前端弹窗时生成随机 imageCodeToken，将本接口地址直接作为 &lt;img&gt; 的 src 展示；
 * 2. 本接口生成验证码图片，答案存入 redis（60 秒有效）；
 * 3. 下单时前端把 imageCode、imageCodeToken 随请求体提交；
 * 4. 网关 LoginMemberFilter 从请求体读取并与 redis 中答案比对，通过才放行。
 */
@RestController
@RequestMapping("/kaptcha")
public class KaptchaController {

    private static final Logger LOG = LoggerFactory.getLogger(KaptchaController.class);

    /**
     * 验证码在 redis 中的 key 前缀，完整 key：BUSINESS:IMAGE:{imageCodeToken}
     * gateway 的 LoginMemberFilter 校验时使用相同前缀（两边代码各自定义了该常量）
     */
    public static final String BUSINESS_IMAGE_CODE = "BUSINESS:IMAGE:";

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    /**
     * 无背景干扰的验证码（@Primary），下单用
     */
    @Resource
    private DefaultKaptcha kaptchaNoBackgroud;

    /**
     * 生成图片验证码。前端把接口地址直接作为 img 的 src，
     * 所以这里直接输出图片字节流，Content-Type: image/jpg
     */
    @GetMapping("/image-code/{imageCodeToken}")
    public void imageCode(@PathVariable String imageCodeToken, HttpServletResponse response) throws Exception {
        // 生成验证码文本与图片
        String text = kaptchaNoBackgroud.createText();
        BufferedImage image = kaptchaNoBackgroud.createImage(text);

        // 答案存入 redis，60 秒过期
        String key = BUSINESS_IMAGE_CODE + imageCodeToken;
        stringRedisTemplate.opsForValue().set(key, text, 60, TimeUnit.SECONDS);
        LOG.info("生成图片验证码：{}，token：{}", text, imageCodeToken);

        // 直接输出图片流
        response.setContentType("image/jpg");
        response.setHeader("Cache-Control", "no-store");
        ImageIO.write(image, "jpg", response.getOutputStream());
    }
}
