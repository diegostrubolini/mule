/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.email.internal.operations;

import static org.apache.commons.collections.CollectionUtils.isEmpty;
import static org.mule.extension.email.internal.util.EmailUtils.getAttributesFromMessage;
import org.mule.extension.email.api.EmailContent;
import org.mule.extension.email.internal.EmailAttributes;
import org.mule.extension.email.internal.builder.MessageBuilder;
import org.mule.extension.email.internal.exception.EmailSenderException;
import org.mule.runtime.api.message.MuleMessage;

import java.util.Calendar;
import java.util.List;

import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.Transport;

/**
 * Represents the reply operation.
 *
 * @since 4.0
 */
public final class ReplyOperation
{

    /**
     * Replies an email message. The message will be sent to the addresses
     * associated to the replyTo attribute in the {@link EmailAttributes} of
     * the incoming {@code muleMessage}.
     * <p>
     * If no email message is found in the incoming {@link MuleMessage} this operation will fail.
     *
     * @param session the {@link Session} through which the message is going to be sent.
     * @param muleMessage the incoming {@link MuleMessage} from which the email is going to getPropertiesInstance the content.
     * @param content the content of the reply message.
     * @param from the person who sends the email.
     * @param replyToAll if this reply should be sent to all recipients of this message,
     *                   or only the sender of the received email.
     */
    public void reply(Session session,
                      MuleMessage muleMessage,
                      EmailContent content,
                      String from,
                      Boolean replyToAll)
    {
        EmailAttributes attributes = getAttributesFromMessage(muleMessage, "Cannot perform the reply operation if no email is provided");

        try
        {
            List<String> replyTo = attributes.getReplyToAddresses();
            if (isEmpty(replyTo))
            {
                replyTo = attributes.getToAddresses();
            }

            Message reply = MessageBuilder.newMessage(session)
                    .fromAddresses(from)
                    .withSubject(attributes.getSubject())
                    .replyTo(replyTo)
                    .withSentDate(Calendar.getInstance().getTime())
                    .build()
                    .reply(replyToAll);

            reply.setContent(content.getBody(), content.getContentType());
            Transport.send(reply);
        }
        catch (MessagingException e)
        {
            throw new EmailSenderException(e);
        }
    }
}
