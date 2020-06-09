package com.fsm.client.interceptor;

import com.fsm.client.constant.FsmConstants;
import com.fsm.client.request.TransactionRequestBean;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class RestTemplateInterceptor implements ClientHttpRequestInterceptor {

    private final TransactionRequestBean transactionRequestBean;

    public RestTemplateInterceptor(TransactionRequestBean transactionRequestBean) {
        this.transactionRequestBean = transactionRequestBean;
    }

    @Override
    public ClientHttpResponse intercept(HttpRequest request, byte[] body, ClientHttpRequestExecution execution) throws IOException {
        request.getHeaders().add(FsmConstants.FSM_TRANSACTION_ID, transactionRequestBean.getTransactionId());
        return execution.execute(request, body);
    }
}
