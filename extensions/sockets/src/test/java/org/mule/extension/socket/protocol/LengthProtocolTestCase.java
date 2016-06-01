/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.socket.protocol;

import org.mule.extension.socket.SocketExtensionTestCase;
import org.mule.tck.junit4.rule.DynamicPort;

import org.junit.Rule;
import org.junit.Test;

public class LengthProtocolTestCase extends SocketExtensionTestCase
{

    public static final String LONG_TEST_STRING = "this is a long test string";
    public static final String SHORT_TEST_STRING = "stringy";

    @Rule
    public DynamicPort dynamicPort = new DynamicPort("port1");

    @Override
    protected String getConfigFile()
    {
        return "length-protocol-config.xml";
    }

    @Test
    public void sendLongerMsgShouldReturnNullPayload() throws Exception
    {
        flowRunner("tcp-write").withPayload(LONG_TEST_STRING).run();
        assertNullPayload(receiveConnection());
    }

    @Test
    public void sendShorterMsgShouldWork() throws Exception
    {
        flowRunner("tcp-write").withPayload(SHORT_TEST_STRING).run();
        assertEvent(receiveConnection(), SHORT_TEST_STRING);
    }
}
