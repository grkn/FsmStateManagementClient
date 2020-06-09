package com.fsm.client.op;

import com.fasterxml.jackson.databind.JsonNode;
import com.fsm.client.constant.FsmEndpoint;
import com.fsm.client.request.TransactionRequestBean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

public class FsmFinalize {
    @Value("${fsm.endpoint}")
    private String endpoint;
    private final TransactionRequestBean transactionRequestBean;
    private final RestTemplate restTemplate;
    private static final Logger LOGGER = LoggerFactory.getLogger(FsmFinalize.class);

    public FsmFinalize(TransactionRequestBean transactionRequestBean, RestTemplate restTemplate) {
        this.transactionRequestBean = transactionRequestBean;
        this.restTemplate = restTemplate;
    }

    public void finalize() {
        LOGGER.debug("FSM finalize method is called by given transaction_id : " + transactionRequestBean.getTransactionId());
        HttpEntity<JsonNode> entity = new HttpEntity<>(null);
        String url = endpoint + FsmEndpoint.DESTROY.getContext().replace("{transaction_id}", transactionRequestBean.getTransactionId());
        ResponseEntity<Void> responseEntity = restTemplate
                .exchange(url, HttpMethod.DELETE, entity, new ParameterizedTypeReference<Void>() {
                });
        if (!responseEntity.getStatusCode().is2xxSuccessful()) {
            throw new IllegalArgumentException("Fsm can not be finalized. Destory Endpoint : " + url);
        }

        LOGGER.debug("FSM finalize method is ended by given transaction_id : " + transactionRequestBean.getTransactionId());
    }
}
