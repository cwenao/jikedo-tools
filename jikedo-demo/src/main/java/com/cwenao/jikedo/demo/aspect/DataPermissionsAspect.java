/*
 * Company
 * Copyright (C) 2014-2021 All Rights Reserved.
 */
package com.cwenao.jikedo.demo.aspect;

import com.cwenao.jikedo.core.annotation.DataPermissions;
import com.cwenao.jikedo.core.aspect.BaseDataPermissionsAspect;
import com.cwenao.jikedo.core.context.DataPermissionsContext;
import com.cwenao.jikedo.core.enumeration.DataPermissionsTypeEnum;
import com.cwenao.jikedo.core.enumeration.SQLParameterEnum;
import com.cwenao.jikedo.core.query.Query;
import com.cwenao.jikedo.core.utils.ObtainAnnotationForJoinPoint;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

/**
 * TODO : Statement the class description
 *
 * @author cwenao
 * @version $Id DataPermissionsAspect.java, v1.0.0 2021-08-31 15:32 cwenao Exp $$
 */
@Slf4j
@Aspect
@Component
@Order(9999)
public class DataPermissionsAspect implements BaseDataPermissionsAspect {

    @Pointcut("@annotation(com.cwenao.jikedo.core.annotation.DataPermissions)")
    public void dataPermissionPointCut(){

    }

    @Before("dataPermissionPointCut()")
    public void doBefore(JoinPoint joinPoint) throws Exception {
        dataPermissionsHandler(joinPoint);
    }

    private void dataPermissionsHandler(final JoinPoint joinPoint) throws Exception {

        DataPermissions dataPermissions = ObtainAnnotationForJoinPoint.getAnnotation(joinPoint,DataPermissions.class);

        if (dataPermissions == null) {
            return;
        }

        generationDataPermissionSQL(joinPoint, dataPermissions);

    }

    @Override
    public void generationDataPermissionSQL(JoinPoint joinPoint, DataPermissions dataPermissions) throws Exception {
        String permission = DataPermissionsContext.getDataPermission();

        //Admin权限有所有的数据权限
        if (DataPermissionsTypeEnum.ADMIN.toString().equals(permission)) {
            return;
        }
        if (dataPermissions.exclude()) {
            return;
        }

        //校验用户权限
        verifyUserDataPermissions(dataPermissions);

        String sql = "";
        if (dataPermissions.groupPermissions() || dataPermissions.onlySelfPermissions()) {
            sql = generationDataPermissionSQLForGroupAndOnlySelfPermissions(dataPermissions);
        }
        if (!StringUtils.isEmpty(sql)) {
            Query query = new Query();
            Map<String,Object> map = new HashMap<>();
            map.put(SQLParameterEnum.CONDITIONS.name(), sql);
            query.setSqlParams(map);
        }
    }

    private String generationDataPermissionSQLForGroupAndOnlySelfPermissions(DataPermissions dataPermissions) {
        StringBuilder sqlString = new StringBuilder();
        List<String> deptList = DataPermissionsContext.listDept();
        String orCondition = " or ";
        if (!CollectionUtils.isEmpty(deptList)) {
            List<String> deptTableAliasNames = Arrays.asList(dataPermissions.deptTableAliasName());
            if (deptTableAliasNames.size() > 0) {
                for (String aliasName : deptTableAliasNames) {
                    String item = aliasName.concat(".");
                    sqlString.append(item).append(dataPermissions.deptTableBizKey()+" in (")
                            .append(StringUtils.collectionToDelimitedString(deptList, ",", "'", "'"))
                            .append(" ) ")
                            .append(orCondition);
                }
            }
            //删除最后的orCondition
            if (sqlString.toString().endsWith(orCondition)) {
                int index = sqlString.lastIndexOf(orCondition);
                sqlString.delete(index, index + orCondition.length());
            }
        } else if (dataPermissions.onlySelfPermissions()) {
            sqlString.append(dataPermissions.employeeTableName().concat(".").concat(dataPermissions.employeeBizKey()))
                    .append("=").append(DataPermissionsContext.getEmployeeCode());
        }
        return sqlString.toString();
    }

    private void verifyUserDataPermissions(DataPermissions dataPermissions) throws Exception {
        if ( !dataPermissions.groupPermissions() && !dataPermissions.onlySelfPermissions()) {
            throw new Exception("当前用户 "+ DataPermissionsContext.getEmployeeCode()+",无数据权限");
        }

        if (StringUtils.isEmpty(DataPermissionsContext.getDataPermission()) ) {
            throw new Exception("当前用户无数据权限, " + "employeeCode:" + DataPermissionsContext.getEmployeeCode());
        }
    }
}
