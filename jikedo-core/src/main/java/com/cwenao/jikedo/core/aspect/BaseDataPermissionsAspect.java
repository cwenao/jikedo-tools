
/*
 * Company
 * Copyright (C) 2014-2021 All Rights Reserved.
 */
package com.cwenao.jikedo.core.aspect;

import com.cwenao.jikedo.core.annotation.DataPermissions;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.reflect.MethodSignature;

/**
 *
 *
 * @author cwenao
 * @version $Id BaseDataPermissionsAspect.java, v1.0.0 2021-08-31 15:20 cwenao Exp $$
 */
public interface BaseDataPermissionsAspect {
    void generationDataPermissionSQL(JoinPoint joinPoint, DataPermissions dataPermissions) throws Exception;
}
