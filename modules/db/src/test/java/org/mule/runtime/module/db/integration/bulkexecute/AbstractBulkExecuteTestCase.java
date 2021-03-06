/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.module.db.integration.bulkexecute;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mule.runtime.module.db.integration.DbTestUtil.selectData;
import static org.mule.runtime.module.db.integration.TestRecordUtil.assertRecords;
import org.mule.runtime.module.db.integration.AbstractDbIntegrationTestCase;
import org.mule.runtime.module.db.integration.model.AbstractTestDatabase;
import org.mule.runtime.module.db.integration.model.Field;
import org.mule.runtime.module.db.integration.model.Record;

import java.sql.SQLException;
import java.util.List;
import java.util.Map;

public abstract class AbstractBulkExecuteTestCase extends AbstractDbIntegrationTestCase
{

    public AbstractBulkExecuteTestCase(String dataSourceConfigResource, AbstractTestDatabase testDatabase)
    {
        super(dataSourceConfigResource, testDatabase);
    }

    protected void assertBulkModeResult(Object payload) throws SQLException
    {
        assertTrue(payload instanceof int[]);
        int[] counters = (int[]) payload;
        assertEquals(2, counters.length);
        assertEquals(0, counters[0]);
        assertEquals(1, counters[1]);

        List<Map<String, String>> result = selectData("select * from PLANET where POSITION=0 or POSITION=4", getDefaultDataSource());
        assertRecords(result, new Record(new Field("NAME", "Mercury"), new Field("POSITION", 4)));
    }
}
