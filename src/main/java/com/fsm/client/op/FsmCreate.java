package com.fsm.client.op;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fsm.client.constant.FsmEndpoint;
import com.fsm.client.context.FsmContextHolder;
import com.fsm.client.request.TransactionRequestBean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;

import java.util.Objects;

public class FsmCreate {

    @Value("${fsm.endpoint}")
    private String endpoint;


    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final TransactionRequestBean transactionRequestBean;
    private static final Logger LOGGER = LoggerFactory.getLogger(FsmCreate.class);

    public FsmCreate(RestTemplate restTemplate, ObjectMapper objectMapper, TransactionRequestBean transactionRequestBean) {
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
        this.transactionRequestBean = transactionRequestBean;
    }

    public void createFsm(String fsmStatesName) {

        if (!StringUtils.isEmpty(transactionRequestBean.getTransactionId())) {
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("Fsm has already create no need to create again");
            }
            return;
        }

        LOGGER.debug("Fsm create method is called by given fmsStateName " + fsmStatesName);
        JsonNode createStatements = FsmContextHolder.getStatesAsMap().get(fsmStatesName);
        HttpEntity<JsonNode> entity = new HttpEntity<>(createStatements);
        ResponseEntity<String> responseEntity = restTemplate
                .exchange(endpoint + FsmEndpoint.CREATE.getContext(),
                        HttpMethod.POST, entity, new ParameterizedTypeReference<String>() {
                        });

        if (responseEntity.getStatusCode().is2xxSuccessful()) {
            try {
                JsonNode jsonNode = objectMapper.readValue(Objects.requireNonNull(responseEntity.getBody()), JsonNode.class);
                if (jsonNode instanceof ObjectNode) {
                    ObjectNode objectNode = (ObjectNode) jsonNode;
                    transactionRequestBean.setTransactionId(objectNode.get("transactionId").textValue());
                }
            } catch (JsonProcessingException e) {
                throw new IllegalArgumentException("Fsm server send not parsable data. Message : " + e.getMessage(), e);
            }
        }
        LOGGER.debug(String.format("Fsm create method is ended by given fmsStateName: %s , transaction_id : %s", fsmStatesName, transactionRequestBean.getTransactionId()));
    }
}
