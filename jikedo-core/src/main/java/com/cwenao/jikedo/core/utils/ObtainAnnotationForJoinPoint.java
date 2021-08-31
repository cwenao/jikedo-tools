/*
 * Company
 * Copyright (C) 2014-2021 All Rights Reserved.
 */
package com.cwenao.jikedo.core.utils;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.reflect.MethodSignature;

/**
 * TODO : Statement the class description
 *
 * @author cwenao
 * @version $Id GetAnnotationForJoinPoint.java, v1.0.0 2021-08-31 15:27 cwenao Exp $$
 */
public class ObtainAnnotationForJoinPoint {

    /**
     * 获取注解信息
     * @param joinPoint
     * @param annotationClazz
     * @param <T>
     * @return
     */
    public static <T extends Annotation> T getAnnotation(JoinPoint joinPoint,Class<? extends Annotation> annotationClazz) {

        MethodSignature methodSignature = (MethodSignature) joinPoint.getSignature();
        Method method = methodSignature.getMethod();

        if (method == null) {
            return null;
        }

        return (T) method.getAnnotation(annotationClazz);
    }

}
