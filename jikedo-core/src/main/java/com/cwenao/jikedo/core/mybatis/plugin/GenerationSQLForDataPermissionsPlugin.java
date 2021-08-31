/*
 * Company
 * Copyright (C) 2014-2021 All Rights Reserved.
 */
package com.cwenao.jikedo.core.mybatis.plugin;

import com.cwenao.jikedo.core.enumeration.SQLParameterEnum;
import com.cwenao.jikedo.core.query.Query;
import com.github.pagehelper.util.ExecutorUtil;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import org.apache.ibatis.cache.CacheKey;
import org.apache.ibatis.executor.Executor;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.mapping.ParameterMapping;
import org.apache.ibatis.plugin.Interceptor;
import org.apache.ibatis.plugin.Intercepts;
import org.apache.ibatis.plugin.Invocation;
import org.apache.ibatis.plugin.Signature;
import org.apache.ibatis.session.ResultHandler;
import org.apache.ibatis.session.RowBounds;

/**
 * 用于生成权限查询sql语句
 * @author cwenao
 * @version $Id GenerationSQLForDataPermissionsPlugin.java, v1.0.0 2021-08-31 13:41 cwenao Exp $$
 */
@SuppressWarnings({"rawtypes", "unchecked"})
@Intercepts(
        {
                @Signature(type = Executor.class, method = "query", args = {MappedStatement.class, Object.class, RowBounds.class, ResultHandler.class}),
                @Signature(type = Executor.class, method = "query", args = {MappedStatement.class, Object.class, RowBounds.class, ResultHandler.class, CacheKey.class, BoundSql.class}),
        }
)
public class GenerationSQLForDataPermissionsPlugin implements Interceptor {

    private static String WHERE_FLAG="WHERE";

    private static String UNION_FLAG="UNION";

    private static String ORDER_FLAG="ORDER";

    @Override
    public Object intercept(Invocation invocation) throws Throwable {

        try {
            Object[] args = invocation.getArgs();
            MappedStatement ms = (MappedStatement) args[0];
            Object parameter = args[1];
            RowBounds rowBounds = (RowBounds) args[2];
            ResultHandler resultHandler = (ResultHandler) args[3];
            Executor executor = (Executor) invocation.getTarget();
            CacheKey cacheKey;
            BoundSql boundSql;

            if (args.length == 4) {
                //4个参数
                boundSql = ms.getBoundSql(parameter);
                cacheKey = executor.createCacheKey(ms, parameter, rowBounds, boundSql);
            } else {
                //6个参数
                boundSql = (BoundSql) args[5];
                cacheKey = (CacheKey) args[4];
            }

            String sql = boundSql.getSql();
            int count = 0;
            if (parameter instanceof Query) {
                Query query = (Query) parameter;
                Map<String, Object> sqlParams = query.getSqlParams();

                if (sqlParams != null && sqlParams.size() > 0) {
                    count = sqlParams.get(SQLParameterEnum.CONDITIONS.name()).toString().split(" or ").length;
                    //解析sql语句
                    sql = parseSQL(sqlParams,sql);
                }
            }

            //组合sql语句
            List<ParameterMapping> parameterMappingList = new ArrayList<>();
            List<ParameterMapping> parameterMappings = boundSql.getParameterMappings();
            parameterMappingList.addAll(parameterMappings);
            for (int i=1;i<count;i++){
                parameterMappingList.addAll(parameterMappings);
            }
            BoundSql tmpBoundSQL = new BoundSql(ms.getConfiguration(), sql,
                    parameterMappingList, boundSql.getParameterObject());

            Map<String, Object> additionalParameters = ExecutorUtil.getAdditionalParameter(boundSql);
            //设置动态参数
            for (String key : additionalParameters.keySet()) {
                tmpBoundSQL.setAdditionalParameter(key, additionalParameters.get(key));
            }

            return executor.query(ms, parameter, rowBounds, resultHandler, cacheKey, tmpBoundSQL);

        } catch (Exception e) {
            e.printStackTrace();
            throw e;
        }
    }

    /**
     * 解析sql
     * TODO 实际需要解析sql而非简单的sql语句拼接
     * @param sqlParams
     * @param sqlStatements
     * @return
     */
    private String parseSQL(Map<String, Object> sqlParams,String sqlStatements) {

        //截取排序前sql语句
        sqlStatements = sqlStatements.replaceAll(" order ", " ORDER ");

        int indexOfForOrder = sqlStatements.lastIndexOf(ORDER_FLAG);
        String orderSql = "";
        if (indexOfForOrder > 0) {
            orderSql = sqlStatements.substring(indexOfForOrder);
            sqlStatements = sqlStatements.substring(0, indexOfForOrder);
        }

        Object conditionObject = sqlParams.get(SQLParameterEnum.CONDITIONS.name());
        String sql = "";

        StringBuffer stringBuffer = new StringBuffer();
        //切分条件参数中的or，转换为union语句
        String[] conditions = conditionObject.toString().split(" or ");
        for (String condition : conditions) {
            //转换为union语句
            if (sqlStatements.indexOf(WHERE_FLAG) > 0) {
                String tmp = WHERE_FLAG + " " + condition + " and ";
                stringBuffer.append(sqlStatements.replace(WHERE_FLAG, tmp) + " " + UNION_FLAG);
            } else {
                stringBuffer.append(sqlStatements + " " + WHERE_FLAG + " "+ condition + " " + UNION_FLAG + " ");
            }
        }

        //删除多余 union
        int index = stringBuffer.lastIndexOf(UNION_FLAG);
        stringBuffer.delete(index,index + UNION_FLAG.length()).toString();
        sql = stringBuffer.toString();

        //去除 union 后order by 表别名
        if (indexOfForOrder>0) {
            sql = sql + orderSql.replaceAll("[a-zA-Z]*\\."," ");
        }

        return sql;
    }

    @Override
    public Object plugin(Object target) {
        return Interceptor.super.plugin(target);
    }

    @Override
    public void setProperties(Properties properties) {
        Interceptor.super.setProperties(properties);
    }
}
