package net.flex.dci.otc.controller.ne.manager.utils;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import lombok.extern.slf4j.Slf4j;

/**
 * @version 1.0
 * @date 2022/3/4 14:32
 */
@Slf4j
public class RetryUtil {

    private static final Integer DEFAULT_RETRY_TIME = 3;

    private static ThreadLocal<Integer> retryThreadLocal = new ThreadLocal<>();

    public static RetryUtil setRetryTimes(Integer retryTimes) {
        log.info("current thread :{}", Thread.currentThread().getId());
        if (retryThreadLocal.get() == null) {
            retryThreadLocal.set(retryTimes);
        }
        return new RetryUtil();
    }

    public Object retry(Object... args) {
        try {
            Integer retryTimes = retryThreadLocal.get();
            if (retryTimes <= 0) {
                retryThreadLocal.remove();
                return null;
            }
            retryThreadLocal.set(--retryTimes);
            String className = Thread.currentThread().getStackTrace()[2].getClassName();
            String methodName = Thread.currentThread().getStackTrace()[2].getMethodName();
            Class<?> clazz = Class.forName(className);
            Object targetObject = clazz.newInstance();
            Method targetMethod = null;
            for (Method method : clazz.getDeclaredMethods()) {
                if (method.getName().equals(methodName)) {
                    targetMethod = method;
                    break;
                }
            }
            if (targetMethod == null) {
                return null;
            }
            targetMethod.setAccessible(true);
            Thread.sleep(3000);
            return targetMethod.invoke(targetObject, args);
        } catch (ClassNotFoundException | InstantiationException | IllegalAccessException | InvocationTargetException | InterruptedException e) {
            log.error("failed to retry the method :{}", e.getMessage(), e);
            e.printStackTrace();
        }
        return null;
    }

}
