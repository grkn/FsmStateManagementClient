package com.fsm.client.aop;

import com.fsm.client.annotation.FsmTrace;
import com.fsm.client.annotation.FsmTraceState;
import com.fsm.client.op.*;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

@Aspect
public class FsmAspect {

    private static final String BEAN_DEF = "#";
    private static final Logger LOGGER = LoggerFactory.getLogger(FsmAspect.class);

    private final FsmCreate fsmCreate;
    private final FsmFinalize fsmFinalize;
    private final FsmSetData fsmSetData;
    private final FsmMoveNextState fsmMoveNextState;
    private final FsmFail fsmFail;

    public FsmAspect(FsmCreate fsmCreate, FsmFinalize fsmFinalize, FsmSetData fsmSetData,
                     FsmMoveNextState fsmMoveNextState, FsmFail fsmFail) {
        this.fsmCreate = fsmCreate;
        this.fsmFinalize = fsmFinalize;
        this.fsmSetData = fsmSetData;
        this.fsmMoveNextState = fsmMoveNextState;
        this.fsmFail = fsmFail;
    }

    @Pointcut("@annotation(com.fsm.client.annotation.FsmTrace)")
    public void beanAnnotatedWithFsmTrace() {
    }

    @Pointcut("@annotation(com.fsm.client.annotation.FsmTraceState)")
    public void beanAnnotationWithFsmTraceState() {
    }

    @Around(value = "beanAnnotationWithFsmTraceState()")
    public Object aroundFsmTraceState(ProceedingJoinPoint joinPoint) throws Throwable {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        FsmTraceState fsmTraceState = method.getAnnotation(FsmTraceState.class);
        String eventName = fsmTraceState.eventName();

        try {
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("Fsm moves to next state. Event Name  : " + eventName);
            }
            fsmMoveNextState.moveNextState(eventName);
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("Fsm fail to move next state. Event Name : " + eventName);
            }

            return joinPoint.proceed();
        } catch (Throwable e) {
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("Fsm fail request will be sent.");
            }
            fsmFail.failFsm();
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("Fsm failed request is dispatched.");
            }
            throw e;
        }
    }

    @Around(value = "beanAnnotatedWithFsmTrace()")
    public Object aroundFsmTrace(ProceedingJoinPoint joinPoint) throws Throwable {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();

        FsmTrace fsmTraceAnnotation = method.getAnnotation(FsmTrace.class);
        String fsmStatesName = fsmTraceAnnotation.fsmStatesName();
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Fsm creation begins with FsmStateName : " + fsmStatesName);
        }

        boolean isCreated = fsmCreate.createFsm(fsmStatesName);

        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Fsm created by given states. FsmStateName" + fsmStatesName);
        }

        Object[] signatureArgs = joinPoint.getArgs();

        String[] requestParams = arrangeParamsByMethodArguments(fsmTraceAnnotation.requestParams(), signatureArgs);
        String[] pathVariables = arrangeParamsByMethodArguments(fsmTraceAnnotation.pathVariable(), signatureArgs);

        fsmSetData.setData(fsmTraceAnnotation.data(), fsmTraceAnnotation.endpoint(), fsmTraceAnnotation.httpMethod(),
                requestParams, pathVariables);

        try {
            Object retVal = joinPoint.proceed();
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("Fsm finalize logic begins.");
            }

            if(isCreated) {
                fsmFinalize.finalize();
            }

            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("Fsm finalize logic ends.");
            }
            return retVal;
        } catch (Throwable e) {
            throw e;
        }
    }

    private String[] arrangeParamsByMethodArguments(String[] variables, Object[] signatureArgs) {
        if (signatureArgs == null || variables == null) {
            return new String[0];
        }

        String[] result = new String[variables.length];
        int resultCounter = 0;
        for (String variable : variables) {
            variable = variable.trim();
            if (variable.contains(BEAN_DEF) && variable.length() > 2) {
                variable = variable.substring(1);
                String[] concatString = variable.split("\\.");
                if (concatString.length > 0) {
                    Object selectedArgument = signatureArgs[0];
                    for (int i = 1; i < concatString.length; i++) {
                        try {
                            Field field = selectedArgument.getClass().getDeclaredField(concatString[i]);
                            field.setAccessible(true);
                            selectedArgument = field.get(selectedArgument);
                            field.setAccessible(false);
                        } catch (NoSuchFieldException e) {
                            throw new IllegalArgumentException(String.format("Argument %s doesn't match with method parameters %s",
                                    concatString[i], selectedArgument.getClass().getSimpleName()));
                        } catch (IllegalAccessException e) {
                            throw new IllegalArgumentException(String.format("The field %s can not be accessed", concatString[i]));
                        }
                    }
                    result[resultCounter] = String.valueOf(selectedArgument);
                }
            } else if (variable.contains(BEAN_DEF) && variable.length() == 2) {
                Integer index = Integer.valueOf(variable.substring(1));
                result[resultCounter] = String.valueOf(signatureArgs[index]);
            } else {
                result[resultCounter] = variable;
            }

            resultCounter++;
        }

        return result;
    }
}
