/*
 * Company
 * Copyright (C) 2014-2021 All Rights Reserved.
 */
package com.cwenao.jikedo.core.configuration;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.integration.redis.util.RedisLockRegistry;

/**
 * TODO : Statement the class description
 *
 * @author cwenao
 * @version $Id RedisLockRegistryConfiguration.java, v1.0.0 2021-08-26 17:19 cwenao Exp $$
 */
@Configuration
@EnableConfigurationProperties(RedisParamConfig.class)
public abstract class RedisLockRegistryConfiguration {

    @Autowired
    private RedisParamConfig redisParamConfig;

    @Bean(name = "repeatRedisLockRegistry")
    public RedisLockRegistry repeatRedisLockRegistry(RedisConnectionFactory connectionFactory) {
        return new RedisLockRegistry(connectionFactory, redisParamConfig.getRegistryKey(),
                redisParamConfig.getRedisLockTimeout() * 1000);
    }

    @Bean(name = "distributedRedisLockRegistry")
    public RedisLockRegistry distributedRedisLockRegistry(RedisConnectionFactory connectionFactory) {
        return new RedisLockRegistry(connectionFactory, redisParamConfig.getDistributedKey(),
                redisParamConfig.getDistributedLockTimeOut() * 1000);
    }
}
