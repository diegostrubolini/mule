/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.email.api.retriever.pop3;

import static org.mule.extension.email.internal.util.EmailConstants.PORT_IMAP;
import static org.mule.extension.email.internal.util.EmailConstants.PROTOCOL_POP3;
import org.mule.extension.email.api.retriever.AbstractRetrieverProvider;
import org.mule.extension.email.api.retriever.RetrieverConnection;
import org.mule.runtime.api.connection.ConnectionException;
import org.mule.runtime.api.connection.ConnectionHandlingStrategy;
import org.mule.runtime.api.connection.ConnectionHandlingStrategyFactory;
import org.mule.runtime.api.connection.ConnectionProvider;
import org.mule.runtime.api.connection.ConnectionValidationResult;
import org.mule.runtime.extension.api.annotation.Alias;
import org.mule.runtime.extension.api.annotation.Parameter;
import org.mule.runtime.extension.api.annotation.param.Optional;

@Alias("pop3")
public class POP3ConnectionProvider extends AbstractRetrieverProvider implements ConnectionProvider<POP3Configuration, RetrieverConnection>
{

    @Parameter
    @Optional(defaultValue = PORT_IMAP)
    private String port;

    @Override
    public RetrieverConnection connect(POP3Configuration config) throws ConnectionException
    {
        return new RetrieverConnection(PROTOCOL_POP3, user, password, host, port, connectionTimeout, readTimeout, writeTimeout, properties, null, config.getFolder());
    }

    @Override
    public void disconnect(RetrieverConnection connection)
    {
        connection.disconnect();
    }

    @Override
    public ConnectionValidationResult validate(RetrieverConnection connection)
    {
        return connection.validate();
    }

    @Override
    public ConnectionHandlingStrategy<RetrieverConnection> getHandlingStrategy(ConnectionHandlingStrategyFactory<POP3Configuration, RetrieverConnection> connectionHandlingStrategyFactory)
    {
        return connectionHandlingStrategyFactory.cached();
    }
}
