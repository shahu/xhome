package com.xhome.common.util;

import org.codehaus.jackson.map.ObjectMapper;

/***
 * 全局的ObjectMapperManager，用单例模式实现
 * @author shahu
 */
public class ObjectMapperManager {

    private ObjectMapperManager() {

    }

    private static ObjectMapper objectMapper;

    public static synchronized ObjectMapper getObjectMapper() {
        if (objectMapper == null) {
            objectMapper = new ObjectMapper();
        }
        return objectMapper;
    }
}
