package com.fsm.client.interceptor;

import com.fsm.client.constant.FsmConstants;
import com.fsm.client.request.TransactionRequestBean;
import feign.RequestInterceptor;
import feign.RequestTemplate;
import org.springframework.stereotype.Component;

@Component
public class FeignInterceptor implements RequestInterceptor {


    private final TransactionRequestBean transactionRequestBean;

    public FeignInterceptor(TransactionRequestBean transactionRequestBean) {
        this.transactionRequestBean = transactionRequestBean;
    }

    @Override
    public void apply(RequestTemplate template) {
        if (transactionRequestBean.getTransactionId() != null) {
            template.header(FsmConstants.FSM_TRANSACTION_ID, transactionRequestBean.getTransactionId());
        }
    }
}
