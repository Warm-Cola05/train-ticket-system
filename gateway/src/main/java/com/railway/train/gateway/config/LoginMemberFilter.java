package com.railway.train.gateway.config;

import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.railway.train.gateway.util.JwtUtil;
import jakarta.annotation.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpRequestDecorator;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;

@Component
public class LoginMemberFilter implements Ordered, GlobalFilter {

    private static final Logger LOG = LoggerFactory.getLogger(LoginMemberFilter.class);

    /**
     * 图片验证码在 redis 中的 key 前缀，与 business 的 KaptchaController.BUSINESS_IMAGE_CODE 保持一致
     */
    private static final String BUSINESS_IMAGE_CODE = "BUSINESS:IMAGE:";

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String path = exchange.getRequest().getURI().getPath();

        // 排除不需要拦截的请求
        if (path.contains("/admin")
                || path.contains("/redis")
                || path.contains("/hello")
                || path.contains("/member/member/login")
                || path.contains("/member/member/send-code")
                || path.contains("/business/kaptcha")) {
            LOG.info("不需要登录验证：{}", path);
            return chain.filter(exchange);
        } else {
            LOG.info("需要登录验证：{}", path);
        }
        // 获取header的token参数
        String token = exchange.getRequest().getHeaders().getFirst("token");
        LOG.info("会员登录验证开始，token：{}", token);
        if (token == null || token.isEmpty()) {
            LOG.info( "token为空，请求被拦截" );
            exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
            return exchange.getResponse().setComplete();
        }

        // 校验token是否有效，包括token是否被改过，是否过期
        boolean validate = JwtUtil.validate(token);
        if (validate) {
            LOG.info("token有效，放行该请求");
            // 购买车票接口：额外校验图片验证码（验证码随请求体提交）
            if (path.contains("/business/confirm-order/do")) {
                return checkImageCode(exchange, chain);
            }
            return chain.filter(exchange);
        } else {
            LOG.warn( "token无效，请求被拦截" );
            exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
            return exchange.getResponse().setComplete();
        }

    }

    /**
     * 图片验证码校验：
     * 前端下单时把 imageCode、imageCodeToken 放在请求体里，网关需要读请求体做校验。
     * WebFlux 中请求体是流、只能读一次，所以先整段读出、校验后再用缓存内容重建请求，
     * 保证后续 business 模块还能正常拿到请求体。
     */
    private Mono<Void> checkImageCode(ServerWebExchange exchange, GatewayFilterChain chain) {
        return DataBufferUtils.join(exchange.getRequest().getBody()).flatMap(dataBuffer -> {
            byte[] bytes = new byte[dataBuffer.readableByteCount()];
            dataBuffer.read(bytes);
            DataBufferUtils.release(dataBuffer);
            String bodyStr = new String(bytes, StandardCharsets.UTF_8);

            JSONObject json = JSONUtil.parseObj(bodyStr);
            String imageCode = json.getStr("imageCode", "");
            String imageCodeToken = json.getStr("imageCodeToken", "");
            String key = BUSINESS_IMAGE_CODE + imageCodeToken;
            String codeInRedis = stringRedisTemplate.opsForValue().get(key);

            boolean pass = codeInRedis != null && codeInRedis.equalsIgnoreCase(imageCode);
            // 用过即删，防止同一个验证码重复提交
            stringRedisTemplate.delete(key);

            if (!pass) {LOG.error("图片验证码校验失败：输入={}，redis答案={}，token={}", imageCode, codeInRedis, imageCodeToken);
                exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
                return exchange.getResponse().setComplete();
            }
            LOG.info("图片验证码校验通过，token：{}", imageCodeToken);

            // 用缓存的内容重建请求体，继续执行后续链路
            // 注意：Spring Cloud Gateway 4 的 ServerHttpRequest.Builder 没有 body() 方法，
            // 必须用 ServerHttpRequestDecorator 包装，重写 getBody()
            ServerHttpRequest newRequest = new ServerHttpRequestDecorator(exchange.getRequest()) {
                @Override
                public Flux<DataBuffer> getBody() {
                    return Flux.just(exchange.getResponse().bufferFactory().wrap(bytes));
                }
            };
            return chain.filter(exchange.mutate().request(newRequest).build());
        });
    }

    /**
     * 优先级设置  值越小  优先级越高
     *
     * @return
     */
    @Override
    public int getOrder() {
        return 0;
    }
}
