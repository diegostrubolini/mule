/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.email.api;

import org.mule.runtime.extension.api.annotation.Parameter;
import org.mule.runtime.extension.api.annotation.param.Optional;
import org.mule.runtime.extension.api.runtime.ConfigurationProvider;

import java.util.Map;

/**
 * Generic contract for all email {@link ConfigurationProvider}s.
 *
 * @since 4.0
 */
public abstract class AbstractEmailProvider
{

    /**
     * The host name of the mail server.
     */
    @Parameter
    protected String host;

    /**
     * Additional custom properties to configure the session.
     */
    @Parameter
    @Optional
    protected Map<String, String> properties;

    /**
     * the client socket connection timeout.
     */
    @Parameter
    @Optional
    protected long connectionTimeout;

    /**
     * the client socket read timeout.
     */
    @Parameter
    @Optional
    protected long readTimeout;

    /**
     * the client socket write timeout.
     */
    @Parameter
    @Optional
    protected long writeTimeout;
}
