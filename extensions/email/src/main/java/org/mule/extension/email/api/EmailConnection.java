/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.email.api;

import org.mule.runtime.api.connection.ConnectionValidationResult;

import javax.mail.Session;

/**
 * Generic contract for an email connection of a connector which operates
 * over the SMTP, IMAP or POP3 protocols.
 *
 * @since 4.0
 */
public interface EmailConnection
{

    /**
     * disconnects the internal client used by the {@link EmailConnection} instance.
     */
    void disconnect();

    /**
     * @return the email {@link Session} used by the connection.
     */
    Session getSession();

    /**
     * Checks if the current {@link EmailConnection} instance is valid or not.
     *
     * @return a {@link ConnectionValidationResult} indicating if the connection
     * is valid or not.
     */
    ConnectionValidationResult validate();
}
