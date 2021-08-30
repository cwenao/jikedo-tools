
/*
 * Company
 * Copyright (C) 2014-2021 All Rights Reserved.
 */
package com.cwenao.jikedo.core.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * TODO : Statement the class description
 *
 * @author cwenao
 * @version $Id RequestRepeatSubmit.java, v1.0.0 2021-08-30 13:51 cwenao Exp $$
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface RequestRepeatSubmitByRedisLockRegistry {

    /**
     * 是否开启
     * 默认true
     * @return
     */
    boolean enable() default true;

    /**
     * 业务编码，用于标识当前业务
     * 不能为null
     * @return
     */
    String bizCode();
}
