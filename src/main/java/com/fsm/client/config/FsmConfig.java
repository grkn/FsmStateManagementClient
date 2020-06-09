package com.fsm.client.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fsm.client.aop.FsmAspect;
import com.fsm.client.filter.FsmTransactionIdFilter;
import com.fsm.client.interceptor.FeignInterceptor;
import com.fsm.client.interceptor.RestTemplateInterceptor;
import com.fsm.client.op.*;
import com.fsm.client.request.TransactionRequestBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;
import org.springframework.context.annotation.ScopedProxyMode;
import org.springframework.web.client.RestTemplate;

import javax.annotation.PostConstruct;
import java.util.Collections;

@Configuration
public class FsmConfig {

    @Autowired(required = false)
    private RestTemplate restTemplate;
    private final ConfigurableApplicationContext applicationContext;

    public FsmConfig(ConfigurableApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
    }

    @PostConstruct
    public void initialize() {
        if (restTemplate == null) {
            restTemplate = applicationContext.getBeanFactory().createBean(RestTemplate.class);
        }
    }

    @Bean
    @Scope(value = "request", proxyMode = ScopedProxyMode.TARGET_CLASS)
    public TransactionRequestBean transactionRequestBean() {
        return new TransactionRequestBean();
    }

    @Bean
    public FeignInterceptor feignInterceptor(TransactionRequestBean transactionRequestBean, ObjectMapper objectMapper) {
        return new FeignInterceptor(transactionRequestBean, objectMapper);
    }

    @Bean
    public RestTemplateInterceptor restTemplateInterceptor(TransactionRequestBean transactionRequestBean) {
        RestTemplateInterceptor restTemplateInterceptor = new RestTemplateInterceptor(transactionRequestBean);
        restTemplate.setInterceptors(Collections.singletonList(restTemplateInterceptor));
        return restTemplateInterceptor;
    }

    @Bean
    public FsmTransactionIdFilter fsmTransactionIdFilter(TransactionRequestBean transactionRequestBean) {
        return new FsmTransactionIdFilter(transactionRequestBean);
    }


    @Bean
    public FsmCreate fsmCreate(TransactionRequestBean transactionRequestBean, ObjectMapper objectMapper) {
        return new FsmCreate(restTemplate, objectMapper, transactionRequestBean);
    }

    @Bean
    public FsmFinalize fsmFinalize(TransactionRequestBean transactionRequestBean) {
        return new FsmFinalize(transactionRequestBean, restTemplate);
    }

    @Bean
    public FsmSetData fsmSetData(TransactionRequestBean transactionRequestBean, ObjectMapper objectMapper) {
        return new FsmSetData(transactionRequestBean, restTemplate, objectMapper);
    }

    @Bean
    public FsmMoveNextState fsmMoveNextState(TransactionRequestBean transactionRequestBean) {
        return new FsmMoveNextState(transactionRequestBean, restTemplate);
    }

    @Bean
    public FsmFail fsmFail(TransactionRequestBean transactionRequestBean) {
        return new FsmFail(transactionRequestBean, restTemplate);
    }

    @Bean
    public FsmAspect fsmAspect(FsmCreate fsmCreate, FsmFinalize fsmFinalize, FsmSetData fsmSetData,
                               FsmMoveNextState fsmMoveNextState, FsmFail fsmFail) {
        return new FsmAspect(fsmCreate, fsmFinalize, fsmSetData, fsmMoveNextState, fsmFail);
    }

}
