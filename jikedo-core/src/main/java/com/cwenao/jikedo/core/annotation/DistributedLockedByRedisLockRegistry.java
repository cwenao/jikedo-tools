
/*
 * Company
 * Copyright (C) 2014-2021 All Rights Reserved.
 */
package com.cwenao.jikedo.core.annotation;

import com.cwenao.jikedo.core.enumeration.DistributedBizKeyTypeEnum;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 分布式锁注解，用于启用分布式锁
 *
 * @author cwenao
 * @version $Id DistributedLockedByRedis.java, v1.0.0 2021-08-26 15:16 cwenao Exp $$
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface DistributedLockedByRedisLockRegistry {

    /**
     * 是否开启
     * @return
     */
    boolean enable() default true;

    /**
     * 业务类型编码：如订单类型可以设置为ORDER
     * 用于区分分布式锁的业务对象
     * @return
     */
    String bizCode();

    /**
     * bizKey所在位置类型：
     * 比如bizKey在Header中传递
     * @return
     */
    DistributedBizKeyTypeEnum bizKeyType();

    /**
     * 分布式锁参数Key，用于获取请求体(body)中参数值
     * 目前只支持两级对象获取 object#bizkey
     * @return
     */
    String bizKey() default "";

    /**
     * 分布式锁超时时间: 单位ms
     * 默认0ms
     * @return
     */
    long timeOut() default 0L;

    /**
     * 是否开启redis锁定
     * 分布式锁获取成功并逻辑执行成功后，使用redis锁定制定时间
     * @return
     */
    boolean enabledRedisLockChecked() default false;


    /**
     * 使用redis锁定制定时间,单位ms
     * 默认20ms
     * @return
     */
    long redisLockCheckedTimeOut() default 20L;

}
