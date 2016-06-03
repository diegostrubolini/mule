/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.email.api.sender;

import static org.mule.runtime.api.connection.ConnectionValidationResult.success;
import org.mule.extension.email.api.AbstractEmailConnection;
import org.mule.runtime.api.connection.ConnectionValidationResult;

import java.util.Map;

/**
 * A connection with a mail server for sending emails.
 *
 * @since 4.0
 */
public class SenderConnection extends AbstractEmailConnection
{

    /**
     * Creates a new instance.
     *
     * @param protocol          the protocol used to send mails.
     * @param username          the username to establish connection with the mail server.
     * @param password          the password corresponding to the {@code username}
     * @param host              the host name of the mail server.
     * @param port              the port number of the mail server.
     * @param connectionTimeout the socket connection timeout
     * @param readTimeout       the socket read timeout
     * @param writeTimeout      the socket write timeout
     * @param properties        the custom properties added to configure the session.
     */
    public SenderConnection(String protocol, String username, String password, String host, String port, long connectionTimeout, long readTimeout, long writeTimeout, Map<String, String> properties)
    {
        super(protocol, username, password, host, port, connectionTimeout, readTimeout, writeTimeout, properties);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void disconnect()
    {
        // No implementation
    }

    /**
     * {@inheritDoc}
     * <p>
     * if the {@link SenderConnection} instance exists, then the validation will always be successful
     */
    @Override
    public ConnectionValidationResult validate()
    {
        return success();
    }

}
