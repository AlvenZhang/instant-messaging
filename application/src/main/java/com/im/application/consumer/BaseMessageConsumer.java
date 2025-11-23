package com.im.application.consumer;

import com.alibaba.fastjson.JSON;
import com.im.common.domain.model.CommonReceiveData;
import lombok.extern.slf4j.Slf4j;

/**
 * 基础消息消费者类
 * 提供将String类型消息转换为CommonReceiveData<T>类型的方法
 */
@Slf4j
public class BaseMessageConsumer {
    
    /**
     * 将String类型的消息转换为CommonReceiveData<T>类型
     * 
     * @param message 消息字符串
     * @param dataType 数据类型
     * @return 转换后的CommonReceiveData对象
     */
    @SuppressWarnings("unchecked")
    public <T> CommonReceiveData<T> getReceiveMessage(String message, Class<T> dataType) {
        try {
            if (message == null || message.trim().isEmpty()) {
                log.error("消息内容为空");
                return null;
            }
            
            log.info("开始转换消息: message={}", message);
            
            // 解析消息为CommonReceiveData
            CommonReceiveData<T> receiveData = JSON.parseObject(message, CommonReceiveData.class);
            
            if (receiveData != null && receiveData.getData() != null) {
                // 将data字段转换为指定的类型
                String dataJson = JSON.toJSONString(receiveData.getData());
                T typedData = JSON.parseObject(dataJson, dataType);
                receiveData.setData(typedData);
            }
            
            log.info("消息转换完成: receiveData={}", receiveData);
            return receiveData;
            
        } catch (Exception e) {
            log.error("消息转换异常: message={}, dataType={}", message, dataType.getName(), e);
            return null;
        }
    }
}
