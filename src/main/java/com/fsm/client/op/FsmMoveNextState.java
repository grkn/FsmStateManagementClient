package com.fsm.client.op;

import com.fsm.client.constant.FsmEndpoint;
import com.fsm.client.request.TransactionRequestBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

public class FsmMoveNextState {

    @Value("${fsm.endpoint}")
    private String endpoint;

    private final TransactionRequestBean transactionRequestBean;
    private final RestTemplate restTemplate;

    public FsmMoveNextState(TransactionRequestBean transactionRequestBean, RestTemplate restTemplate) {
        this.transactionRequestBean = transactionRequestBean;
        this.restTemplate = restTemplate;
    }

    public void moveNextState(String eventName) {
        final String url = endpoint + FsmEndpoint.MOVE_NEXT_STATE.getContext()
                .replace("{transaction_id}", transactionRequestBean.getTransactionId())
                .replace("{eventName}", eventName);

        ResponseEntity<String> responseEntity = restTemplate
                .exchange(url, HttpMethod.GET, new HttpEntity<>(null), new ParameterizedTypeReference<String>() {
                });

        if (!responseEntity.getStatusCode().is2xxSuccessful()) {
            throw new IllegalArgumentException(String.format("Fsm can not move to next state with eventName : %s. transactionId: %s",
                    eventName, transactionRequestBean.getTransactionId()));
        }
    }
}
