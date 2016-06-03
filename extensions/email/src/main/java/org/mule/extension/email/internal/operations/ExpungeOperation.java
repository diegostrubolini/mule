/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.email.internal.operations;

import static javax.mail.Folder.READ_WRITE;

import org.mule.extension.email.api.retriever.RetrieverConnection;
import org.mule.extension.email.internal.exception.EmailRetrieverException;

import javax.mail.MessagingException;

/**
 * Represents the expunge folder operation.
 *
 * @since 4.0
 */
public class ExpungeOperation
{

    /**
     * Removes from the mailbox all deleted messages
     * if the flag is setted true.
     *
     * @param connection the associated {@link RetrieverConnection}.
     */
    public void expunge(RetrieverConnection connection)
    {
        try
        {
            connection.getOpenFolder(READ_WRITE).close(true);
        }
        catch (MessagingException me)
        {
            throw new EmailRetrieverException(me);
        }
    }
}
