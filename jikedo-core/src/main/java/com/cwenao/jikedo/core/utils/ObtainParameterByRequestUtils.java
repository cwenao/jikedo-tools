/*
 * Company
 * Copyright (C) 2014-2021 All Rights Reserved.
 */
package com.cwenao.jikedo.core.utils;

import cn.hutool.core.util.HashUtil;
import cn.hutool.core.util.ReflectUtil;
import com.cwenao.jikedo.core.enumeration.DistributedBizKeyTypeEnum;
import java.io.IOException;
import java.util.Enumeration;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import org.apache.commons.lang3.ArrayUtils;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.HandlerMapping;

/**
 * TODO : Statement the class description
 *
 * @author cwenao
 * @version $Id ObtainLockKeyByRequest.java, v1.0.0 2021-08-31 10:58 cwenao Exp $$
 */
public class ObtainParameterByRequestUtils {

    public static String getLockKeyByRequest(HttpServletRequest request, ProceedingJoinPoint joinPoint,
            DistributedBizKeyTypeEnum bizKeyType,String bizKey) {

        String lockedKey = null;

        if (DistributedBizKeyTypeEnum.HEADER.equals(bizKeyType)) {
            lockedKey = request.getHeader(bizKey);
        } else if (DistributedBizKeyTypeEnum.PATHVARIABLE.equals(bizKeyType)) {
            Map uriMap = (Map) request.getAttribute(HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE);
            if (uriMap != null) {
                lockedKey = (String) uriMap.get(bizKey);
            }
        }else if (DistributedBizKeyTypeEnum.BODY.equals(bizKeyType)){
            if (RequestMethod.GET.toString().equals(request.getMethod())) {
                lockedKey = request.getParameter(bizKey);
            }else {
                lockedKey = getLockKeyByArgs(joinPoint, bizKey);
            }
        }else if (DistributedBizKeyTypeEnum.INTERNAL.equals(bizKeyType)){
            lockedKey = getLockKeyByArgs(joinPoint, bizKey);
        }else {
            throw new RuntimeException("分布式锁参数类型不存在：" + bizKey + "，类型："+bizKeyType);
        }
        if (StringUtils.isEmpty(lockedKey)) {
            throw new RuntimeException("分布式锁参数 bizKey：" + bizKey + " 获取失败！");
        }
        return lockedKey;
    }


    /**
     * 对request参数信息进行hash
     * @param request
     * @param bodyArgs
     * @return
     * @throws IOException
     */
    public static Long requestPathHash(HttpServletRequest request,String bodyArgs) throws IOException {

        StringBuffer sb = new StringBuffer();
        sb.append(request.getRequestURI()).append("?");
        Enumeration<?> temp = request.getParameterNames();
        while (temp.hasMoreElements()) {
            String paramName = (String) temp.nextElement();
            sb.append(paramName).append("=").append(request.getParameter(paramName)).append("&");
        }
        Enumeration<?> temp1 = request.getHeaderNames();
        while (temp1.hasMoreElements()) {
            String key = (String) temp1.nextElement();
            sb.append(key).append("=").append(request.getParameter(key)).append("&");
        }
        if (!StringUtils.isEmpty(bodyArgs)) {
            sb.append("bodyargs").append("=").append(bodyArgs);
        }

        return HashUtil.mixHash(sb.toString());
    }

    private static String getLockKeyByArgs(ProceedingJoinPoint joinPoint, String bizKey) {
        //获取参数列表
        MethodSignature methodSignature = (MethodSignature) joinPoint.getSignature();
        String[] parameterNames = methodSignature.getParameterNames();

        //获取参数值列表
        Object[] args = joinPoint.getArgs();

        //解析bizKey
        String[] keys = bizKey.split("#");
        String paramKey = null;
        int bizKeyIndex = -1;

        if (keys.length > 1) {
            bizKey = keys[0];
            paramKey = keys[keys.length - 1];
        }

        if ((bizKeyIndex = ArrayUtils.indexOf(parameterNames, bizKey)) == -1) {
            throw new RuntimeException("获取body args参数：" + bizKey + " 不存在");
        }

        Object argsObject = args[bizKeyIndex];

        if (!StringUtils.isEmpty(paramKey)) {
            return String.valueOf( ReflectUtil.getFieldValue(argsObject, paramKey));
        } else {
            return String.valueOf(argsObject);
        }
    }
}
