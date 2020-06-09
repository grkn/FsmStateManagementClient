package com.fsm.client;

import com.fsm.client.aop.FsmAspect;
import com.fsm.client.config.FsmConfig;
import org.springframework.context.annotation.Import;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Import({FsmConfig.class})
public @interface EnableFsmClient {
}
