package io.github.oljc.arcoserve.shared.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.TreeMap;

public final class JsonUtils {

    private static final ObjectMapper MAPPER = new ObjectMapper()
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
            .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
            .setSerializationInclusion(JsonInclude.Include.NON_NULL);

    private JsonUtils() {}

    public static String toJson(Object o) {
        try {
            return MAPPER.writeValueAsString(o);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("JSON序列化失败", e);
        }
    }

    public static <T> T fromJson(String json, Class<T> clazz) {
        try {
            return MAPPER.readValue(json, clazz);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("JSON反序列化失败", e);
        }
    }

    public static <T> T fromJson(String json, TypeReference<T> typeRef) {
        try {
            return MAPPER.readValue(json, typeRef);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("JSON泛型反序列化失败", e);
        }
    }

    public static boolean isValidJson(String json) {
        try {
            return json != null && !json.isBlank() && MAPPER.readTree(json) != null;
        } catch (Exception e) {
            return false;
        }
    }

    public static String toSortedJson(Object o) {
        try {
            JsonNode root = MAPPER.valueToTree(o);
            JsonNode sorted = sortNode(root);
            return MAPPER.writeValueAsString(sorted);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("JSON排序序列化失败", e);
        }
    }

    private static JsonNode sortNode(JsonNode node) {
        if (node.isObject()) {
            var sortedMap = new TreeMap<String, JsonNode>();
            node.fieldNames().forEachRemaining(f -> sortedMap.put(f, sortNode(node.get(f))));
            ObjectNode obj = MAPPER.createObjectNode();
            sortedMap.forEach(obj::set);
            return obj;
        } else if (node.isArray()) {
            ArrayNode array = MAPPER.createArrayNode();
            node.forEach(n -> array.add(sortNode(n)));
            return array;
        } else {
            return node;
        }
    }
}
