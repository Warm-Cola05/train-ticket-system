package com.railway.train.business.enums;

/**
 * RocketMQ 主题枚举：统一管理项目中的消息主题
 */
public enum RocketMQTopicEnum {

    CONFIRM_ORDER("CONFIRM_ORDER", "确认订单");

    private String code;

    private String desc;

    RocketMQTopicEnum(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getDesc() {
        return desc;
    }

    public void setDesc(String desc) {
        this.desc = desc;
    }

    @Override
    public String toString() {
        return "RocketMQTopicEnum{" +
                "code='" + code + '\'' +
                ", desc='" + desc + '\'' +
                "} " + super.toString();
    }
}
