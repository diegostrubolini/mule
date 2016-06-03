/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.email.api.retriever;

import org.mule.extension.email.api.retriever.imap.IMAPConfiguration;
import org.mule.extension.email.internal.EmailAttributes;
import org.mule.extension.email.internal.operations.ExpungeOperation;
import org.mule.extension.email.internal.operations.ListOperation;
import org.mule.runtime.api.message.MuleMessage;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.extension.api.annotation.param.Connection;
import org.mule.runtime.extension.api.annotation.param.UseConfig;

import java.util.List;

import javax.inject.Inject;

/**
 * A set of operations for all email configurations that aims to list emails.
 *
 * @since 4.0
 */
public class RetrieverOperations
{
    //TODO: DELETE THIS WHEN MULE MESSAGE DOENST REQUIRE THE CONTEXT TO BE BUILT WITH ATTRIBUTES.
    @Inject
    private MuleContext context;

    private final ListOperation listOperation = new ListOperation();
    private final ExpungeOperation expungeOperation = new ExpungeOperation();

    //TODO: ADD MATCHER.
    //TODO: ADD PAGINATION SUPPORT WHEN AVAILABLE
    /**
     * List all the emails in the configured folder.
     *
     * @param connection  the corresponding {@link RetrieverConnection} instance.
     * @return a {@link List} of {@link MuleMessage} carrying all the emails and it's corresponding attributes.
     */
    public List<MuleMessage<String, EmailAttributes>> list(@UseConfig IMAPConfiguration config, @Connection RetrieverConnection connection)
    {
        return listOperation.list(connection, context, config.isEagerlyFetchContent());
    }

    /**
     * Removes from the mailbox all deleted messages
     * if the flag is setted true.
     *
     * @param connection the associated {@link RetrieverConnection}.
     */
    public void expunge(@Connection RetrieverConnection connection)
    {
        expungeOperation.expunge(connection);
    }
}
