/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.email.internal.operations;

import static java.lang.String.format;
import static javax.mail.Folder.READ_WRITE;
import static org.mule.extension.email.internal.util.EmailUtils.getAttributesFromMessage;
import org.mule.extension.email.api.retriever.RetrieverConnection;
import org.mule.extension.email.internal.exception.EmailException;
import org.mule.runtime.api.message.MuleMessage;

import java.util.List;

import javax.mail.Flags.Flag;
import javax.mail.Folder;
import javax.mail.Message;
import javax.mail.MessagingException;

/**
 * Represents the set flag operation.
 *
 * @since 4.0
 */
public class SetFlagOperation
{

    private static final String NO_ID_ERROR_MASK = "Expecting an explicit emailId value or email attributes " +
                                                   "in the incoming mule message in order to set the [%s] flag.";

    /**
     * Sets the specified {@code flag} into an email or {@link List} of emails
     * that are being carried in the incoming {@link MuleMessage}
     * <p>
     * if no email(s) are found in the incoming {@link MuleMessage} the operation
     * will fetch the email for the specified {@code emailId}.
     * <p>
     * If no email(s) are found in the {@link MuleMessage} and no {@code emailId} is specified.
     * the operation will fail.
     *
     * @param muleMessage the incoming {@link MuleMessage}.
     * @param connection  the associated {@link RetrieverConnection}.
     * @param emailId     the optional number of the email to be marked. for default the email is taken from the incoming {@link MuleMessage}.
     * @param flag        the flag to be setted.
     */
    public void set(MuleMessage muleMessage, RetrieverConnection connection, Integer emailId, Flag flag)
    {
        Folder folder = connection.getOpenFolder(READ_WRITE);
        Object payload = muleMessage.getPayload();
        if (payload instanceof List)
        {
            for (Object o : (List) payload)
            {
                if (o instanceof MuleMessage)
                {
                    setFlag((MuleMessage) o, folder, emailId, flag);
                }
                else
                {
                    throw new EmailException("Cannot perform operation for the incoming payload");
                }
            }
        }
        else
        {
            setFlag(muleMessage, folder, emailId, flag);
        }
        connection.closeFolder(false);
    }

    /**
     * Sets the specified {@code flag} into an email.
     * <p>
     * The email is taken from the incoming {@link MuleMessage}, if no email is found in the {@link MuleMessage}
     * is going to be fetched from the folder by the specified {@code emailNumber}. If no email was found in the  {@link MuleMessage}
     * and the {@code emailNumber} was not specified the operation will fail.
     *
     * @param muleMessage the incoming {@link MuleMessage}.
     * @param folder      the configured folder.
     * @param emailId     the optional number of the email to be marked. for default the email is taken from the incoming {@link MuleMessage}.
     * @param flag        the flag to be setted.
     */
    private void setFlag(MuleMessage muleMessage, Folder folder, Integer emailId, Flag flag)
    {
        if (emailId == null)
        {
            emailId = getAttributesFromMessage(muleMessage, format(NO_ID_ERROR_MASK, flag.toString())).getId();
        }

        try
        {
            Message message = folder.getMessage(emailId);
            message.setFlag(flag, true);
        }
        catch (MessagingException e)
        {
            throw new EmailException(format("Error while fetching email id [%s] in order to set the flag [%s]", emailId, flag.toString()), e);
        }
    }
}
