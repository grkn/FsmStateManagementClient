package com.fsm.client.filter;

import com.fsm.client.constant.FsmConstants;
import com.fsm.client.request.TransactionRequestBean;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;

@Order(1)
public class FsmTransactionIdFilter implements Filter{

    private final TransactionRequestBean transactionRequestBean;

    public FsmTransactionIdFilter(TransactionRequestBean transactionRequestBean) {
        this.transactionRequestBean = transactionRequestBean;
    }

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {

    }

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain) throws IOException, ServletException {
        HttpServletRequest httpServletRequest = (HttpServletRequest) servletRequest;
        transactionRequestBean.setTransactionId(httpServletRequest.getHeader(FsmConstants.FSM_TRANSACTION_ID));
        filterChain.doFilter(servletRequest,servletResponse);
    }

    @Override
    public void destroy() {

    }
}
