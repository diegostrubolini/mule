/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.email.api;

import static org.mule.extension.email.internal.EmailPropertiesFactory.getPropertiesInstance;
import org.mule.extension.email.internal.PasswordAuthenticator;

import java.util.Map;
import java.util.Properties;

import javax.mail.Session;

public abstract class AbstractEmailConnection implements EmailConnection
{

    protected final Session session;

    /**
     * Base constructor for {@link AbstractEmailConnection} implementations.
     *
     * @param protocol          the protocol used to send mails.
     * @param user              the username to establish connection with the mail server.
     * @param password          the password corresponding to the {@code username}
     * @param host              the host name of the mail server.
     * @param port              the port number of the mail server.
     * @param connectionTimeout the socket connection timeout
     * @param readTimeout       the socket read timeout
     * @param writeTimeout      the socket write timeout
     * @param properties        the custom properties added to configure the session.
     */
    public AbstractEmailConnection(String protocol,
                                   String user,
                                   String password,
                                   String host,
                                   String port,
                                   long connectionTimeout,
                                   long readTimeout,
                                   long writeTimeout,
                                   Map<String, String> properties)
    {
        Properties connectionProperties = getPropertiesInstance(protocol, host, port, connectionTimeout, readTimeout, writeTimeout, properties);
        PasswordAuthenticator authenticator = null;
        if (user != null && password != null)
        {
            authenticator = new PasswordAuthenticator(user, password);
        }

        session = Session.getInstance(connectionProperties, authenticator);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Session getSession()
    {
        return session;
    }

}
