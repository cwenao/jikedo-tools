
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
 * @version $Id DataPermissions.java, v1.0.0 2021-08-31 13:06 cwenao Exp $$
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(value = ElementType.METHOD)
public @interface DataPermissions {

    /**
     * 组织机构表别名，一条sql中可能存在多个别名
     * @return
     */
    String[] deptTableAliasName() default {};

    /**
     * 用于查询组织机构的字段
     * @return
     */
    String deptTableBizKey() default "";

    /**
     * 雇员相关表名称
     * @return
     */
    String employeeTableName() default "";

    /**
     * 雇员相关表查询字段
     * @return
     */
    String employeeBizKey() default "";

    /**
     * 仅自己可见权限
     * @return
     */
    boolean onlySelfPermissions() default false;

    /**
     * group可见权限
     * @return
     */
    boolean groupPermissions() default false;

    /**
     * 不进行权限校验
     * @return
     */
    boolean exclude() default false;
}
