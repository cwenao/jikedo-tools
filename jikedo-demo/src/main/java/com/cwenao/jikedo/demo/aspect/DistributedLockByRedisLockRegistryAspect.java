/*
 * Company
 * Copyright (C) 2014-2021 All Rights Reserved.
 */
package com.cwenao.jikedo.demo.aspect;

import com.cwenao.jikedo.core.annotation.DistributedLockedByRedisLockRegistry;
import com.cwenao.jikedo.core.aspect.BaseRedisLockAspect;
import com.cwenao.jikedo.core.configuration.RedisParamConfig;
import com.cwenao.jikedo.core.enumeration.DistributedBizKeyTypeEnum;
import com.cwenao.jikedo.core.utils.ObtainAnnotationForJoinPoint;
import com.cwenao.jikedo.core.utils.ObtainParameterByRequestUtils;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import javax.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.core.annotation.Order;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.integration.redis.util.RedisLockRegistry;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * 基于redisLockRegistry实现
 *
 * @author cwenao
 * @version $Id RedisDistributedLockAspect.java, v1.0.0 2021-08-27 10:18 cwenao Exp $$
 */
@Slf4j
@Aspect
@Component
@EnableConfigurationProperties(RedisParamConfig.class)
@Order(9999)
public class DistributedLockByRedisLockRegistryAspect implements BaseRedisLockAspect {

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

        DistributedLockedByRedisLockRegistry distributedLockByRedis =
                ObtainAnnotationForJoinPoint.getAnnotation(joinPoint,
                DistributedLockedByRedisLockRegistry.class);

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

            String lockedKey = ObtainParameterByRequestUtils.getLockKeyByRequest(request,joinPoint,bizKeyType,bizKey);

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
     * @param redisKey
     * @param redisValue
     * @param timeOut
     * @param timeUnit
     */
    @Override
    public void checkedRedisLockCache(String redisKey, String redisValue, long timeOut, TimeUnit timeUnit) {
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
     * @param redisKey
     * @param redisLockCheckedTimeOut
     * @param milliseconds
     */
    @Override
    public void setRedisLockCache(String redisKey, long redisLockCheckedTimeOut, TimeUnit milliseconds){
        redisTemplate.expire(redisKey, redisLockCheckedTimeOut, TimeUnit.MILLISECONDS);
    }

    /**
     * 更新reids 缓存过期时间
     * @param redisKey
     */
    @Override
    public void updatedRedisLockCache(String redisKey){
        redisTemplate.delete(redisKey);
    }


    /**
     * 调用返回结果处理，如接口返回错误代码处理
     * @param result
     */
    @Override
    public void bizResultProcessor(Object result){
        BaseRedisLockAspect.super.bizResultProcessor(result);
    }
}
