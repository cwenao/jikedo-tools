
/*
 * Company
 * Copyright (C) 2014-2021 All Rights Reserved.
 */
package com.cwenao.jikedo.core.query;

import java.io.Serializable;
import java.util.Map;
import lombok.Getter;
import lombok.Setter;

/**
 * TODO : Statement the class description
 *
 * @author cwenao
 * @version $Id Query.java, v1.0.0 2021-08-31 14:22 cwenao Exp $$
 */
public class Query implements Serializable {

    @Getter
    @Setter
    private Map<String, Object> sqlParams;
}
