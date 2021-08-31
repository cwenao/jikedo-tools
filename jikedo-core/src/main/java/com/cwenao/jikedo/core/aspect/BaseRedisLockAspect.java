/*
 * Company
 * Copyright (C) 2014-2021 All Rights Reserved.
 */
package com.cwenao.jikedo.core.aspect;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.concurrent.TimeUnit;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.reflect.MethodSignature;

/**
 * TODO : Statement the class description
 *
 * @author cwenao
 * @version $Id BaseAspect.java, v1.0.0 2021-08-31 10:24 cwenao Exp $$
 */

public interface BaseRedisLockAspect {
    /**
     * 分布式事务成功后，需要在redis缓存当前标识
     * 用于三方系统间单据处理标记
     * @param redisKey
     * @param redisValue
     * @param timeOut
     * @param timeUnit
     */
    public default void checkedRedisLockCache(String redisKey, String redisValue,long timeOut,TimeUnit timeUnit){};


    /**
     * 设置reids 缓存过期时间
     * @param redisKey
     * @param redisLockCheckedTimeOut
     * @param milliseconds
     */
    public default void setRedisLockCache(String redisKey, long redisLockCheckedTimeOut, TimeUnit milliseconds){};

    /**
     * 更新reids 缓存过期时间
     * @param redisKey
     */
    public default void updatedRedisLockCache(String redisKey){};

    /**
     * 调用返回结果处理，如接口返回错误代码处理
     * @param result
     */
    public default void bizResultProcessor(Object result){};

}
