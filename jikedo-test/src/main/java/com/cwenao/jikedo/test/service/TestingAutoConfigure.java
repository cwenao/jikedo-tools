/*
 * Company
 * Copyright (C) 2014-2021 All Rights Reserved.
 */
package com.cwenao.jikedo.test.service;

import org.springframework.stereotype.Service;

/**
 * TODO : Statement the class description
 *
 * @author cwenao
 * @version $Id TestingAutoConfigure.java, v1.0.0 2021-08-31 09:45 cwenao Exp $$
 */
@Service
public class TestingAutoConfigure {

    public String testAutoConfigure(String testAutoConfigure) {
        System.out.println(testAutoConfigure);
        return testAutoConfigure;
    }

}
