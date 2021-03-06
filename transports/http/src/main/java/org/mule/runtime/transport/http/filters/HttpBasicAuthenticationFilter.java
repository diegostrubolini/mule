/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.transport.http.filters;

import org.mule.runtime.core.api.MuleEvent;
import org.mule.runtime.core.api.security.Authentication;
import org.mule.runtime.core.api.security.SecurityContext;
import org.mule.runtime.core.api.security.SecurityException;
import org.mule.runtime.core.api.security.SecurityProviderNotFoundException;
import org.mule.runtime.core.api.security.UnauthorisedException;
import org.mule.runtime.transport.http.HttpConstants;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;


/**
 * <code>HttpBasicAuthenticationFilter</code> TODO
 */
public class HttpBasicAuthenticationFilter extends org.mule.runtime.module.http.internal.filter.HttpBasicAuthenticationFilter
{
    /**
     * logger used by this class
     */
    protected static final Log logger = LogFactory.getLog(HttpBasicAuthenticationFilter.class);

    public HttpBasicAuthenticationFilter(String realm)
    {
        this.setRealm(realm);
    }

    /**
     * Authenticates the current message if authenticate is set to true. This method
     * will always populate the secure context in the session
     *
     * @param event the current event being dispatched
     * @throws org.mule.api.security.SecurityException if authentication fails
     */
    public void authenticateOutbound(MuleEvent event)
            throws SecurityException, SecurityProviderNotFoundException
    {
        SecurityContext securityContext = event.getSession().getSecurityContext();
        if (securityContext == null)
        {
            if (isAuthenticate())
            {
                throw new UnauthorisedException(event, securityContext, this);
            }
            else
            {
                return;
            }
        }

        Authentication auth = securityContext.getAuthentication();
        if (isAuthenticate())
        {
            auth = getSecurityManager().authenticate(auth);
            if (logger.isDebugEnabled())
            {
                logger.debug("Authentication success: " + auth.toString());
            }
        }

        StringBuilder header = new StringBuilder(128);
        header.append("Basic ");
        String token = auth.getCredentials().toString();
        header.append(new String(Base64.encodeBase64(token.getBytes())));

        event.getMessage().setOutboundProperty(HttpConstants.HEADER_AUTHORIZATION, header.toString());
    }
}
