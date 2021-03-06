/*
 * Company
 * Copyright (C) 2014-2021 All Rights Reserved.
 */
package com.cwenao.jikedo.demo.aspect;

import com.alibaba.fastjson.JSON;
import com.cwenao.jikedo.core.annotation.RequestRepeatSubmitByRedisLockRegistry;
import com.cwenao.jikedo.core.aspect.BaseRedisLockAspect;
import com.cwenao.jikedo.core.configuration.RedisParamConfig;
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
 * TODO : Statement the class description
 *
 * @author cwenao
 * @version $Id RequestRepeatSubmitByRedisLockRegistryAspect.java, v1.0.0 2021-08-30 13:58 cwenao Exp $$
 */
@Slf4j
@Aspect
@Component
@EnableConfigurationProperties(RedisParamConfig.class)
@Order(99999)
public class RequestRepeatSubmitByRedisLockRegistryAspect implements BaseRedisLockAspect {

    @Autowired
    private RedisParamConfig redisParamConfig;

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Autowired
    private RedisLockRegistry repeatRedisLockRegistry;

    public static final String REPEAT_SUBMIT_HEADER_TICKET = "repeatTicket";

    public static final String DEFAULT_BIZ_CODE="rlw_repeat_ticket";

    public static final long DEFAULT_REDIS_TRY_LOCK_TIME = 500;

    public static final long DEFAULT_TICKET_SUCCESS_TIME_OUT = 60;

    @Pointcut("@annotation(com.cwenao.jikedo.core.annotation.RequestRepeatSubmitByRedisLockRegistry)")
    public void repeatCheck() {
    }

    @Around("repeatCheck()")
    public Object repeatSubmitCheck(ProceedingJoinPoint joinPoint) throws Throwable {

        RequestRepeatSubmitByRedisLockRegistry requestRepeatSubmitByRedisLockRegistry =
                ObtainAnnotationForJoinPoint.getAnnotation(joinPoint,RequestRepeatSubmitByRedisLockRegistry.class);

        if (requestRepeatSubmitByRedisLockRegistry != null && requestRepeatSubmitByRedisLockRegistry.enable()) {

            RequestAttributes requestAttributes = RequestContextHolder.getRequestAttributes();
            ServletRequestAttributes servletRequestAttributes = (ServletRequestAttributes) requestAttributes;
            HttpServletRequest request = servletRequestAttributes.getRequest();

            String submitTicket = request.getHeader(REPEAT_SUBMIT_HEADER_TICKET);
            String storeCode = request.getHeader("storeCode");

            String bizCode = null;

            Long ticketTimeOut = redisParamConfig == null || redisParamConfig.getTicketTimeOut() == null ?
                    DEFAULT_TICKET_SUCCESS_TIME_OUT : redisParamConfig.getTicketTimeOut();

            if (!StringUtils.isEmpty(requestRepeatSubmitByRedisLockRegistry.bizCode())) {
                bizCode = requestRepeatSubmitByRedisLockRegistry.bizCode().toString();
            } else {
                bizCode = DEFAULT_BIZ_CODE;
            }

            if (StringUtils.isEmpty(submitTicket)) {
                boolean enableHash = redisParamConfig.isEnableRequestPathHash();

                ticketTimeOut = redisParamConfig == null || redisParamConfig.getNoneTicketTimeOut() == null ?
                        DEFAULT_TICKET_SUCCESS_TIME_OUT : redisParamConfig.getNoneTicketTimeOut();

                String bodyArgs = null ;
                try {
                    bodyArgs = JSON.toJSONString(joinPoint.getArgs());
                } catch (Exception e) {
                    log.error("??????body args????????????: " + e.getMessage());
                }
                Long requestPathHash = ObtainParameterByRequestUtils.requestPathHash(request,bodyArgs);

                if (requestPathHash == null || !enableHash) {
                    throw new Exception("????????????????????????repeatTicket?????????null,???????????????????????????");
                }
                submitTicket = storeCode + "_" + requestPathHash;
            }
            String keyCode = bizCode + "_" + submitTicket;
            if (log.isDebugEnabled()) {
                log.debug("?????????????????????repeatTicket: {}, bizCode: {}, keyCode: {}", submitTicket, bizCode, keyCode);
            }

            String tmp = redisTemplate.opsForValue().getAndSet(keyCode, submitTicket);
            if (!StringUtils.isEmpty(tmp)) {
                if (log.isDebugEnabled()) {
                    log.debug("????????????????????????{} ??????????????????????????????" ,keyCode );
                }
                //??????????????????
                redisTemplate.expire(keyCode, ticketTimeOut, TimeUnit.SECONDS);
                throw new Exception("??????????????????????????????????????????");
            }
            //RedisLockRegistry
            return repeatCheckByRedisLockRegistry(joinPoint, keyCode, submitTicket,ticketTimeOut);

        }

        return joinPoint.proceed();
    }

    private Object repeatCheckByRedisLockRegistry(ProceedingJoinPoint joinPoint,String keyCode,String submitTicket,
            Long ticketTimeOut) throws Throwable {

        Lock lock = null;
        try {
            lock = repeatRedisLockRegistry.obtain(submitTicket);
            if (!lock.tryLock(
                    redisParamConfig.getTryLockTimeOut() == null ? DEFAULT_REDIS_TRY_LOCK_TIME : redisParamConfig.getTryLockTimeOut(),
                    TimeUnit.MILLISECONDS)) {
                log.info("????????????????????????{} ??????????????????????????????", submitTicket);
                throw new Exception("???????????????????????????????????????");
            }
            log.info("?????????????????????,??????redisLockRegistry?????????{}", submitTicket);

            Object result = joinPoint.proceed();
            //??????????????????
            bizResultProcessor(result);
            //??????????????????
            redisTemplate.expire(keyCode, ticketTimeOut, TimeUnit.SECONDS);

            return result;
        } catch (Exception e) {
            log.error("????????????????????????{} ?????????????????????{} ,????????????????????????",keyCode, e.getMessage());
            e.printStackTrace();

            redisTemplate.delete(keyCode);

            throw e;
        }finally {
            log.info("?????????????????????, ??????redisLockRegistry???{}", submitTicket);
            if (lock != null) {
                try {
                    lock.unlock();
                } catch (Exception e) {
                    log.error("????????????????????????{} ??????RedisLockRegistry??????????????????", submitTicket);
                }
            }
        }
    }

    /**
     * ??????????????????
     * @param result
     */
    @Override
    public void bizResultProcessor(Object result){};
}
