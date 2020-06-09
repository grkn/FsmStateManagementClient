package com.fsm.client.interceptor;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fsm.client.constant.FsmConstants;
import com.fsm.client.request.TransactionRequestBean;
import feign.RequestInterceptor;
import feign.RequestTemplate;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;

@Component
public class FeignInterceptor implements RequestInterceptor {


    private final TransactionRequestBean transactionRequestBean;
    private ObjectMapper objectMapper;

    public FeignInterceptor(TransactionRequestBean transactionRequestBean, ObjectMapper objectMapper) {
        this.transactionRequestBean = transactionRequestBean;
        this.objectMapper = objectMapper;
    }

    @PostConstruct
    public void init() {
        if (objectMapper == null) {
            objectMapper = new ObjectMapper();
        }
    }

    @Override
    public void apply(RequestTemplate template) {
        if (transactionRequestBean.getTransactionId() != null) {
            template.header(FsmConstants.FSM_TRANSACTION_ID, transactionRequestBean.getTransactionId());
        }
    }
}
