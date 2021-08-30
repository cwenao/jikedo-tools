/*
 * Company
 * Copyright (C) 2014-2021 All Rights Reserved.
 */
package com.cwenao.jikedo.core.configuration;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * TODO : Statement the class description
 *
 * @author cwenao
 * @version $Id RedisParamConfig.java, v1.0.0 2021-08-27 09:30 cwenao Exp $$
 */
@Data
@ConfigurationProperties(prefix = "jikedo")
public class RedisParamConfig {

    /**
     * 防重复提交票据过期时间
     * 默认30秒
     */
    private Long ticketTimeOut = 30L;

    /**
     * 尝试获取redisLockRegistry锁等待时间
     * 默认0秒
     */
    private Long tryLockTimeOut = 0L;

    /**
     * RedisLockRegistry key 过期时间
     * 默认60秒
     */
    private Long redisLockTimeout=60L;

    /**
     * RedisLockRegistry 锁仓库前缀
     * 默认 jikedo_
     */
    private String registryKey="jikedo_";

    /**
     * 无法获取防重复提交票据时，使用request进行hash
     */
    private boolean enableRequestPathHash=true;

    /**
     * 使用非票据进行防重复时，过期时间
     * 默认30秒
     */
    private Long noneTicketTimeOut=30L;

    /**
     * 分布式锁 前缀
     */
    private String distributedKey = "jikedo";

    /**
     * 分布式锁超时时间
     * 默认60秒
     */
    private Long distributedLockTimeOut = 60L;

    public static void main(String[] args) {
        Integer test = Integer.getInteger("1");
        System.out.println(test==1);
    }
}
