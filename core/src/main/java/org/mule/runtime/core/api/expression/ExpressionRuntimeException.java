/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.api.expression;

import org.mule.runtime.core.api.MuleRuntimeException;
import org.mule.runtime.core.config.i18n.Message;

/**
 * If thrown by the {@link org.mule.runtime.core.expression.DefaultExpressionManager} if an expression returns null
 * and failIfNull was set when {@link ExpressionManager#evaluate(String,org.mule.runtime.core.api.MuleMessage,boolean)}
 * was called.
 */
public class ExpressionRuntimeException extends MuleRuntimeException
{
    /**
     * @param message the exception message
     */
    public ExpressionRuntimeException(Message message)
    {
        super(message);
    }

    /**
     * @param message the exception message
     * @param cause   the exception that triggered this exception
     */
    public ExpressionRuntimeException(Message message, Throwable cause)
    {
        super(message, cause);
    }
}
