/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.module.db.integration.config;

import org.mule.runtime.module.db.integration.TestDbConfig;
import org.mule.runtime.module.db.integration.model.AbstractTestDatabase;
import org.mule.runtime.module.db.integration.model.OracleTestDatabase;

import java.util.Collections;
import java.util.List;

import org.junit.runners.Parameterized;

public class OracleDatasourceConfigTestCase extends AbstractDatabaseConfigTestCase
{

    public OracleDatasourceConfigTestCase(String dataSourceConfigResource, AbstractTestDatabase testDatabase)
    {
        super(dataSourceConfigResource, testDatabase);
    }

    @Parameterized.Parameters
    public static List<Object[]> parameters()
    {
        if (TestDbConfig.getOracleResource().isEmpty())
        {
            return Collections.emptyList();
        }
        else
        {
            return Collections.singletonList(new Object[] {"integration/config/oracle-datasource-config.xml", new OracleTestDatabase()});
        }
    }
}
