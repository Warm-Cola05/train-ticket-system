package com.railway.train.business.mq;

import com.alibaba.fastjson.JSON;
import com.railway.train.business.req.ConfirmOrderDoReq;
import com.railway.train.business.service.ConfirmOrderService;
import jakarta.annotation.Resource;
import org.apache.rocketmq.common.message.MessageExt;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;

/**
 * 确认订单消费者：收到消息后执行真正的选座购票逻辑。
 * 消费者由 RocketMQ 推送消息被动触发，不需要主动调用。
 *
 * 注意：注解的属性值必须是编译期常量，所以 topic 这里直接写字符串，
 * 与 RocketMQTopicEnum.CONFIRM_ORDER 保持一致。
 */
@Component
@RocketMQMessageListener(topic = "CONFIRM_ORDER", selectorExpression = "do", consumerGroup = "confirm-order-group")
public class ConfirmOrderConsumer implements RocketMQListener<MessageExt> {

    private static final Logger LOG = LoggerFactory.getLogger(ConfirmOrderConsumer.class);

    @Resource
    private ConfirmOrderService confirmOrderService;

    @Override
    public void onMessage(MessageExt messageExt) {
        byte[] body = messageExt.getBody();
        String json = new String(body, StandardCharsets.UTF_8);
        LOG.info("ROCKETMQ收到消息：{}", json);
        ConfirmOrderDoReq req = JSON.parseObject(json, ConfirmOrderDoReq.class);
        // 同转异后打印 logId，方便把一次购票的前置校验和异步出票两段日志串起来跟踪
        LOG.info("ROCKETMQ收到消息，logId：{}", req.getLogId());
        confirmOrderService.sell(req);
    }
}
