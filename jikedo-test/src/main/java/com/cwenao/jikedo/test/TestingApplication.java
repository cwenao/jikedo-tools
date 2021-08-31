/*
 * Company
 * Copyright (C) 2014-2021 All Rights Reserved.
 */
package com.cwenao.jikedo.test;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * TODO : Statement the class description
 *
 * @author cwenao
 * @version $Id TestingApplication.java, v1.0.0 2021-08-31 09:54 cwenao Exp $$
 */
@SpringBootApplication(scanBasePackages = {"com.cwenao.jikedo","com.cwenao.jikedo.demo.aspect"})
public class TestingApplication {

    public static void main(String[] args) {
        SpringApplication.run(TestingApplication.class,args);
    }
}
