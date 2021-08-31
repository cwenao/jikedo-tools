/*
 * Company
 * Copyright (C) 2014-2021 All Rights Reserved.
 */
package com.cwenao.jikedo.core.context;

import java.util.List;
import org.apache.commons.lang3.ObjectUtils;

/**
 * 数据权限当前线程上下文
 * 可以在拦截器中进行设值
 *
 * @author cwenao
 * @version $Id DataPermissionsContext.java, v1.0.0 2021-08-31 14:06 cwenao Exp $$
 */
public class DataPermissionsContext {

    private static ThreadLocal<DataPermission> dataPermissionContext = new ThreadLocal();

    /**
     * 权限类
     */
    private static class DataPermission{
        //用户雇员编码
        String employeeCode;

        //拥有的最大权限
        String dataPermission;

        //所在组织机构列表
        List<String> deptList;

        public DataPermission(String employeeCode,String dataPermission, List<String> deptList) {
            this.employeeCode = employeeCode;
            this.dataPermission = dataPermission;
            this.deptList = deptList;
        }
    }


    public static void set(String employeeCode,String dataPermission, List<String> deptList){
        DataPermission dp = new DataPermission(employeeCode,dataPermission, deptList);
        dataPermissionContext.set(dp);
    }

    public static String getEmployeeCode() {
        try {
            if (dataPermissionContext == null || ObjectUtils.isEmpty(dataPermissionContext.get().employeeCode)) {
                throw new Exception("No employeeCode in Context");
            }
            return dataPermissionContext.get().employeeCode;
        } catch (Exception e) {
            return null;
        }
    }

    public static String getDataPermission() {
        try {
            if (dataPermissionContext == null || ObjectUtils.isEmpty(dataPermissionContext.get().dataPermission)) {
                throw new Exception("No dataPermission in Context");
            }
            return dataPermissionContext.get().dataPermission;
        } catch (Exception e) {
            return null;
        }
    }


    public static List<String> listDept() {
        try {
            if (dataPermissionContext == null || ObjectUtils.isEmpty(dataPermissionContext.get().deptList)) {
                throw new Exception("No deptList in Context");
            }
            return dataPermissionContext.get().deptList;
        } catch (Exception e) {
            return null;
        }
    }

    public static void remove() {
        dataPermissionContext.remove();
    }
}
