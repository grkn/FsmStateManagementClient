package com.fsm.client.context;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class FsmContextHolder {
    private static JsonNode states = null;
    private static Map<String, JsonNode> statesAsMap = new HashMap<>();

    static {
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            byte[] arr = new byte[1024 * 1024 * 5];
            InputStream inputStream = Thread.currentThread().getContextClassLoader().getResourceAsStream("states.json");
            int length = 0;
            length = inputStream.read(arr);
            states = objectMapper.readValue(new String(arr, 0, length, StandardCharsets.UTF_8), JsonNode.class);

            Iterator<JsonNode> elements = ((ArrayNode) states).elements();
            while (elements.hasNext()) {
                ObjectNode next = (ObjectNode) elements.next();
                Iterator<Map.Entry<String, JsonNode>> fields = next.fields();
                while (fields.hasNext()) {
                    Map.Entry<String, JsonNode> obj = fields.next();
                    statesAsMap.put(obj.getKey(), obj.getValue());
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static JsonNode getStates() {
        return states;
    }

    public static Map<String, JsonNode> getStatesAsMap() {
        return statesAsMap;
    }

}
