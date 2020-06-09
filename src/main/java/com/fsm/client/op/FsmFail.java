package com.fsm.client.op;

import com.fsm.client.constant.FsmEndpoint;
import com.fsm.client.request.TransactionRequestBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

public class FsmFail {

    @Value("${fsm.endpoint}")
    private String endpoint;

    private final TransactionRequestBean transactionRequestBean;
    private final RestTemplate restTemplate;

    public FsmFail(TransactionRequestBean transactionRequestBean, RestTemplate restTemplate) {
        this.transactionRequestBean = transactionRequestBean;
        this.restTemplate = restTemplate;
    }

    public void failFsm() {
        final String url = endpoint + FsmEndpoint.FAIL.getContext()
                .replace("{transaction_id}", transactionRequestBean.getTransactionId());

        ResponseEntity<String> responseEntity = restTemplate
                .exchange(url, HttpMethod.PUT, new HttpEntity<>(null), new ParameterizedTypeReference<String>() {
                });

        if (!responseEntity.getStatusCode().is2xxSuccessful()) {
            throw new IllegalArgumentException(String.format("Fsm can not be failed. Transaction id : %s ",
                    transactionRequestBean.getTransactionId()));
        }
    }
}
