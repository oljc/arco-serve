package io.github.oljc.arcoserve.shared.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * JSON工具类
 */
public interface JsonUtils {

    Logger log = LoggerFactory.getLogger(JsonUtils.class);
    ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    /**
     * 对象转JSON字符串
     */
    static String toJsonString(Object obj) {
        try {
            return OBJECT_MAPPER.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            log.error("对象转JSON失败", e);
            return "{}";
        }
    }

    /**
     * JSON字符串转对象
     */
    static <T> T fromJsonString(String json, Class<T> clazz) {
        try {
            return OBJECT_MAPPER.readValue(json, clazz);
        } catch (JsonProcessingException e) {
            log.error("JSON转对象失败", e);
            return null;
        }
    }
}
