/*
 * Company
 * Copyright (C) 2014-2021 All Rights Reserved.
 */
package com.cwenao.jikedo.test.controller;

import com.cwenao.jikedo.core.annotation.RequestRepeatSubmitByRedisLockRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

/**
 * TODO : Statement the class description
 *
 * @author cwenao
 * @version $Id Test.java, v1.0.0 2021-08-30 16:23 cwenao Exp $$
 */
@Slf4j
@RestController
@RequestMapping(value ="/test")
public class Test {

    @RequestRepeatSubmitByRedisLockRegistry(bizCode = "test-cwenao")
    @RequestMapping(value ="/test",method = RequestMethod.GET)
    public String test(String test) {
        log.info("this is the test string: {}", test);
        return test;
    }

}
