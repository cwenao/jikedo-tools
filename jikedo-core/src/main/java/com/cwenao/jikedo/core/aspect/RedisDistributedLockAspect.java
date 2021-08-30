/*
 * Company
 * Copyright (C) 2014-2021 All Rights Reserved.
 */
package com.cwenao.jikedo.core.aspect;

import cn.hutool.core.util.ReflectUtil;
import com.cwenao.jikedo.core.annotation.DistributedLockedByRedisLockRegistry;
import com.cwenao.jikedo.core.enumeration.DistributedBizKeyTypeEnum;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import javax.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ArrayUtils;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.Order;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.integration.redis.util.RedisLockRegistry;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.servlet.HandlerMapping;

/**
 * 基于redisLockRegistry实现
 *
 * @author cwenao
 * @version $Id RedisDistributedLockAspect.java, v1.0.0 2021-08-27 10:18 cwenao Exp $$
 */
@Slf4j
@Aspect
@Component
@Order(9999)
public abstract class RedisDistributedLockAspect {

    @Autowired
    private RedisLockRegistry distributedRedisLockRegistry;

    private static final String DEFAULT_BIZ_CODE = "jikedo_locked_";

    @Autowired
    private StringRedisTemplate redisTemplate;


    @Pointcut("@annotation(com.cwenao.jikedo.core.annotation.DistributedLockedByRedisLockRegistry)")
    public void distributedLock() {
    }

    @Around("distributedLock()")
    public Object execute(ProceedingJoinPoint joinPoint) throws Throwable {

        DistributedLockedByRedisLockRegistry distributedLockByRedis = getAnnotation(joinPoint);

        if (distributedLockByRedis != null && distributedLockByRedis.enable()) {

            RequestAttributes requestAttributes = RequestContextHolder.getRequestAttributes();
            ServletRequestAttributes servletRequestAttributes = (ServletRequestAttributes) requestAttributes;
            HttpServletRequest request = servletRequestAttributes.getRequest();

            DistributedBizKeyTypeEnum bizKeyType = distributedLockByRedis.bizKeyType();

            String bizCode = distributedLockByRedis.bizCode();
            String bizKey = distributedLockByRedis.bizKey();
            Long timeOut = distributedLockByRedis.timeOut();

            if (StringUtils.isEmpty(bizKeyType.toString())) {
                throw new RuntimeException("分布式锁注解: bizKeyType不能为空");
            }

            String lockedKey = getLockKeyByRequest(request,joinPoint,bizKeyType,bizKey);

            //启用redis锁
            if (distributedLockByRedis.enabledRedisLockChecked()) {
                String redisKey = (StringUtils.isEmpty(bizCode) ? DEFAULT_BIZ_CODE : bizCode+"_") + lockedKey;

                checkedRedisLockCache(redisKey,redisKey,distributedLockByRedis.redisLockCheckedTimeOut(),
                        TimeUnit.MILLISECONDS);
            }

            log.info("==================开始获取分布式锁===============，bizCode: {},bizKey: {}, lockedKey: {}, bizKeyType: {},"
                            + "timeOut: {}",
                    bizCode,bizKey,
                    lockedKey, bizKeyType,
                    timeOut);
            return distributedLockByRedisLockRegistry(joinPoint,distributedLockByRedis, bizCode, lockedKey, timeOut);

        }
        return joinPoint.proceed();
    }


    private String getLockKeyByRequest(HttpServletRequest request,ProceedingJoinPoint joinPoint,
            DistributedBizKeyTypeEnum bizKeyType,String bizKey) {

        String lockedKey = null;

        if (DistributedBizKeyTypeEnum.HEADER.equals(bizKeyType)) {
            lockedKey = request.getHeader(bizKey);
        } else if (DistributedBizKeyTypeEnum.PATHVARIABLE.equals(bizKeyType)) {
            Map uriMap = (Map) request.getAttribute(HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE);
            if (uriMap != null) {
                lockedKey = (String) uriMap.get(bizKey);
            }
        }else if (DistributedBizKeyTypeEnum.BODY.equals(bizKeyType)){
            if (RequestMethod.GET.toString().equals(request.getMethod())) {
                lockedKey = request.getParameter(bizKey);
            }else {
                lockedKey = getLockKeyByArgs(joinPoint, bizKey);
            }
        }else if (DistributedBizKeyTypeEnum.INTERNAL.equals(bizKeyType)){
            lockedKey = getLockKeyByArgs(joinPoint, bizKey);
        }else {
            throw new RuntimeException("分布式锁参数类型不存在：" + bizKey + "，类型："+bizKeyType);
        }
        if (StringUtils.isEmpty(lockedKey)) {
            throw new RuntimeException("分布式锁参数 bizKey：" + bizKey + " 获取失败！");
        }
        return lockedKey;
    }

    private String getLockKeyByArgs(ProceedingJoinPoint joinPoint, String bizKey) {
        //获取参数列表
        MethodSignature methodSignature = (MethodSignature) joinPoint.getSignature();
        String[] parameterNames = methodSignature.getParameterNames();

        //获取参数值列表
        Object[] args = joinPoint.getArgs();

        //解析bizKey
        String[] keys = bizKey.split("#");
        String paramKey = null;
        int bizKeyIndex = -1;

        if (keys.length > 1) {
            bizKey = keys[0];
            paramKey = keys[keys.length - 1];
        }

        if ((bizKeyIndex = ArrayUtils.indexOf(parameterNames, bizKey)) == -1) {
            throw new RuntimeException("获取body args参数：" + bizKey + " 不存在");
        }

        Object argsObject = args[bizKeyIndex];

        if (!StringUtils.isEmpty(paramKey)) {
            return String.valueOf( ReflectUtil.getFieldValue(argsObject, paramKey));
        } else {
            return String.valueOf(argsObject);
        }

    }

    /**
     *
     * @param joinPoint
     * @param bizCode
     * @param lockedKey
     * @param timeOut
     * @return
     * @throws Throwable
     */
    private Object distributedLockByRedisLockRegistry(ProceedingJoinPoint joinPoint,
            DistributedLockedByRedisLockRegistry distributedLockedByRedisLockRegistry,
            String bizCode,String lockedKey,
            Long timeOut) throws Throwable{
        Lock lock = null;
        String redisKey = (StringUtils.isEmpty(bizCode) ? DEFAULT_BIZ_CODE : bizCode+"_") + lockedKey;

        try {
            lock = distributedRedisLockRegistry.obtain(redisKey);
            if (!lock.tryLock(timeOut, TimeUnit.MILLISECONDS)) {
                log.error("获取分布式锁失败：{} 请稍后再试", redisKey);
                throw new RuntimeException("单据: "+lockedKey+"正在操作，请稍后再试");
            }
            Object result = joinPoint.proceed();

            bizResultProcessor(result);

            //启用redis锁
            if (distributedLockedByRedisLockRegistry.enabledRedisLockChecked()) {
                setRedisLockCache(redisKey, distributedLockedByRedisLockRegistry.redisLockCheckedTimeOut(),TimeUnit.MILLISECONDS);
            }
            return result;

        } catch (Exception e) {
            log.error("单据号：{} 业务执行失败：{} ,稍后释放分布式锁",redisKey, e.getMessage());
            e.printStackTrace();
            if (distributedLockedByRedisLockRegistry.enabledRedisLockChecked()) {
                log.info("=========单据号：{}业务执行失败，开始删除redis锁定状态==============", redisKey);
                updatedRedisLockCache(redisKey);
            }
            throw e;
        }finally {
            log.info("释放分布式锁 {}", redisKey);
            if (lock != null) {
                try {
                    lock.unlock();
                } catch (Exception e) {
                    log.error("释放分布式锁{}失败: {}", redisKey,e.getMessage());
                }
            }
        }
    }

    /**
     * 分布式事务成功后，需要在redis缓存当前标识
     * 用于三方系统间单据处理标记
     * @see #defaultCheckedRedisLockCache
     * @param redisKey
     * @param redisValue
     * @param timeOut
     * @param timeUnit
     */
    protected abstract void checkedRedisLockCache(String redisKey, String redisValue,long timeOut,TimeUnit timeUnit);

    /**
     * 默认实现，用于参考
     * @param redisKey
     * @param redisValue
     */
    protected  void defaultCheckedRedisLockCache(String redisKey, String redisValue,Long timeOut,TimeUnit timeUnit){

        if (!StringUtils.isEmpty(redisTemplate.opsForValue().getAndSet(redisKey, redisValue))) {
            log.error("=========开始获取分布式锁-确认redis中已存在===============，redisKey: {},redisValue: {}",
                    redisKey,redisValue);

            //重置过期时间
            redisTemplate.expire(redisKey, timeOut, timeUnit);

            throw new RuntimeException("当前单据：" + redisKey + " 正在操作中，请稍后再试");
        }
    }


    /**
     * 设置reids 缓存过期时间
     * @see #defaultSetRedisLockCache
     * @param redisKey
     * @param redisLockCheckedTimeOut
     * @param milliseconds
     */
    protected abstract void setRedisLockCache(String redisKey, long redisLockCheckedTimeOut, TimeUnit milliseconds);

    public void defaultSetRedisLockCache(String redisKey, long redisLockCheckedTimeOut, TimeUnit milliseconds){
        redisTemplate.expire(redisKey, redisLockCheckedTimeOut, TimeUnit.MILLISECONDS);
    }

    /**
     * 更新reids 缓存过期时间
     * @see #defaultUpdatedRedisLockCache
     * @param redisKey
     */
    protected abstract void updatedRedisLockCache(String redisKey);

    public void defaultUpdatedRedisLockCache(String redisKey){
        redisTemplate.delete(redisKey);
    }


    /**
     * 调用返回结果处理，如接口返回错误代码处理
     * @param result
     */
    abstract void bizResultProcessor(Object result);

    private DistributedLockedByRedisLockRegistry getAnnotation(JoinPoint joinPoint) {

        MethodSignature methodSignature = (MethodSignature) joinPoint.getSignature();
        Method method = methodSignature.getMethod();

        if (method == null) {
            return null;
        }

        return method.getAnnotation(DistributedLockedByRedisLockRegistry.class);
    }

}
