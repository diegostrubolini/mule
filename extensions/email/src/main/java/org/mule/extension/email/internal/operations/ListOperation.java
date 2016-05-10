/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.email.internal.operations;

import static javax.mail.Folder.READ_ONLY;
import static org.mule.extension.email.internal.builder.EmailAttributesBuilder.fromMessage;
import static org.mule.extension.email.internal.util.EmailUtils.getBody;
import org.mule.extension.email.api.retriever.RetrieverConnection;
import org.mule.extension.email.internal.EmailAttributes;
import org.mule.extension.email.internal.exception.EmailRetrieverException;
import org.mule.runtime.api.message.MuleMessage;
import org.mule.runtime.core.DefaultMuleMessage;
import org.mule.runtime.core.api.MuleContext;

import java.util.LinkedList;
import java.util.List;

import javax.mail.Folder;
import javax.mail.Message;
import javax.mail.MessagingException;

/**
 * Represents the list emails operation.
 *
 * @since 4.0
 */
public class ListOperation
{

    /**
     * Retrieves all the emails in the specified {@code folder}.
     *
     * A new {@link MuleMessage} is created for each fetched email from the folder, where
     * the payload is the text body of the email and the other metadata is carried by
     * an {@link EmailAttributes} instance.
     *
     * For folder implementations (like IMAP) that support fetching without reading the content, if the content
     * should NOT be read ({@code readContent} = false) the SEEN flag is not going to be setted.
     *
     * @param connection the associated {@link RetrieverConnection}.
     * @param context the mule context to create the result message.
     * @param readContent if should read the email content or not.
     * @return a {@link List} of {@link MuleMessage} carrying all the emails and it's corresponding attributes.
     */
    public List<MuleMessage<String, EmailAttributes>> list(RetrieverConnection connection, MuleContext context, boolean readContent)
    {
        Folder folder = connection.getOpenFolder(READ_ONLY);
        try
        {
            List<MuleMessage<String, EmailAttributes>> list = new LinkedList<>();
            for (Message m : folder.getMessages())
            {
                MuleMessage muleMessage = readContent ? new DefaultMuleMessage(getBody(m), null, fromMessage(m), context)
                                                      : new DefaultMuleMessage("", null, fromMessage(m, false), context);

                list.add(muleMessage);
            }
            connection.closeFolder(false);
            return list;
        }
        catch (MessagingException me)
        {
            throw new EmailRetrieverException(me);
        }
    }
}
