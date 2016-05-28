/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.socket.api.client;

import org.mule.module.socket.internal.SocketDelegate;
import org.mule.runtime.api.connection.ConnectionException;
import org.mule.runtime.core.api.context.MuleContextAware;

import java.io.IOException;
import java.util.Optional;

public interface ListenerSocket extends SocketClient, MuleContextAware
{
    Optional<SocketDelegate> receive() throws ConnectionException, IOException;
}