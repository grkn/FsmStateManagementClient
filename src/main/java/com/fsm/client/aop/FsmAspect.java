package com.fsm.client.aop;

import com.fsm.client.annotation.FsmTrace;
import com.fsm.client.annotation.FsmTraceState;
import com.fsm.client.context.FsmContextHolder;
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
import java.util.Collection;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Aspect
public class FsmAspect {

    private static final String BEAN_DEF = "#";
    private static final Logger LOGGER = LoggerFactory.getLogger(FsmAspect.class);
    private static final Pattern KEY_PATTERN = Pattern.compile("(\\w*)(\\[)('*)(\\w*)('*)(\\])");
    private static final Pattern ENDPOINT_URL = Pattern.compile("(\\$\\{)([\\w.]*)(\\})");

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

        String endpoint = findEndpoint(fsmTraceAnnotation.endpoint());

        fsmSetData.setData(fsmTraceAnnotation.data(), endpoint, fsmTraceAnnotation.httpMethod(),
                requestParams, pathVariables);

        try {
            Object retVal = joinPoint.proceed();
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("Fsm finalize logic begins.");
            }

            if (isCreated) {
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

    private String findEndpoint(String endpoint) {
        Matcher matcher = ENDPOINT_URL.matcher(endpoint);
        if(matcher.find()){
            String key = matcher.group(2);
            return FsmContextHolder.getPropertyByKey(key);
        }
        return endpoint;
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
                            if (isIterable(concatString[i])) {
                                String str = concatString[i].substring(0, concatString[i].indexOf("["));
                                selectedArgument = handleObject(str, selectedArgument);
                                concatString[i] = concatString[i].substring(concatString[i].indexOf("["), concatString[i].length());
                            }
                            boolean isArray = selectedArgument.getClass().isArray();
                            boolean isCollection = Collection.class.isAssignableFrom(selectedArgument.getClass());
                            boolean isMap = Map.class.isAssignableFrom(selectedArgument.getClass());

                            if (isArray) {
                                selectedArgument = handleArray(concatString[i], selectedArgument);
                            } else if (isCollection) {
                                selectedArgument = handleCollection(concatString[i], selectedArgument);
                            } else if (isMap) {
                                selectedArgument = handleMap(concatString[i], selectedArgument);
                            } else {
                                selectedArgument = handleObject(concatString[i], selectedArgument);
                            }
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

    private boolean isIterable(String string) {
        return string.contains("[") && string.contains("]");
    }

    private Object handleObject(String name, Object selectedArgument) throws NoSuchFieldException, IllegalAccessException {
        Field field = selectedArgument.getClass().getDeclaredField(name);
        field.setAccessible(true);
        selectedArgument = field.get(selectedArgument);
        field.setAccessible(false);
        return selectedArgument;
    }

    private Object handleMap(String input, Object selectedArgument) {
        Matcher matcher = KEY_PATTERN.matcher(input);
        if (matcher.find()) {
            String key = matcher.group(4);
            Map map = (Map) selectedArgument;
            selectedArgument = map.get(key);
        }
        return selectedArgument;
    }

    private Object handleCollection(String input, Object selectedArgument) {
        Matcher matcher = KEY_PATTERN.matcher(input);
        if (matcher.find()) {
            int index = Integer.parseInt(matcher.group(4));
            Collection collection = (Collection) selectedArgument;
            selectedArgument = collection.toArray()[index];
        }
        return selectedArgument;
    }

    private Object handleArray(String input, Object selectedArgument) {
        Matcher matcher = KEY_PATTERN.matcher(input);
        if (matcher.find()) {
            int index = Integer.parseInt(matcher.group(4));
            Object[] arr = (Object[]) selectedArgument;
            selectedArgument = arr[index];
        }
        return selectedArgument;
    }
}
