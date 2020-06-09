package com.fsm.client.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface FsmTrace {
    String fsmStatesName();

    String endpoint();

    String[] pathVariable() default {};

    String data() default "";

    String[] requestParams() default {};

    String httpMethod();
}
