package com.fsm.client.op;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fsm.client.constant.FsmEndpoint;
import com.fsm.client.request.TransactionRequestBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;

import java.text.MessageFormat;

public class FsmSetData {

    @Value("${fsm.endpoint}")
    private String endpoint;

    private final TransactionRequestBean transactionRequestBean;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    public FsmSetData(TransactionRequestBean transactionRequestBean, RestTemplate restTemplate, ObjectMapper objectMapper) {
        this.transactionRequestBean = transactionRequestBean;
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
    }

    public void setData(String data, String revertEndpoint, String method, String[] requestParams, String[] pathVariables) {
        String url = this.endpoint + FsmEndpoint.SET_DATA.getContext()
                .replace("{transaction_id}", transactionRequestBean.getTransactionId());

        if (requestParams.length > 0 || pathVariables.length > 0) {
            String[] pathRequestVariables = fillParameterArray(requestParams, pathVariables);
            revertEndpoint = MessageFormat.format(revertEndpoint, pathRequestVariables);
        }

        ObjectNode body = new ObjectNode(JsonNodeFactory.instance);
        try {
            if (!StringUtils.isEmpty(data)) {
                body.set("data", objectMapper.readValue(data, JsonNode.class));
            }
            body.put("revertEndpoint", revertEndpoint);
            body.put("httpMethod", method.toUpperCase());
            HttpEntity<JsonNode> httpEntity = new HttpEntity<>(body);
            ResponseEntity<Void> responseEntity = restTemplate
                    .exchange(url, HttpMethod.PUT, httpEntity, new ParameterizedTypeReference<Void>() {
                    });

            if (!responseEntity.getStatusCode().is2xxSuccessful()) {
                throw new IllegalArgumentException(
                        String.format("Set Data operation fails with given Data: %s, Endpoint: %s , Method : %s", data, revertEndpoint, method));
            }
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Json processing error in FsmSetData class. Data can not be converted JsonNode", e);
        }


    }

    private String[] fillParameterArray(String[] requestParams, String[] pathVariables) {
        String[] requestPathVariables = new String[pathVariables.length + requestParams.length];
        for (int i = 0, j = 0; i < requestPathVariables.length; i++) {
            if (i < pathVariables.length) {
                requestPathVariables[i] = pathVariables[i];

            } else if (j < requestParams.length) {
                requestPathVariables[i] = requestParams[j];
                j++;
            }
        }
        return requestPathVariables;
    }
}
