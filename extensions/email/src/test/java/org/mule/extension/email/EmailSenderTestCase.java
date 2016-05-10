package org.mule.extension.email;/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

import static java.lang.String.format;
import static java.util.Collections.singletonList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.Is.isA;
import static org.hamcrest.core.IsInstanceOf.instanceOf;
import static org.hamcrest.core.StringContains.containsString;
import static org.mule.extension.email.internal.builder.EmailAttributesBuilder.fromMessage;
import static org.mule.extension.email.internal.util.EmailUtils.ATTRIBUTES_NOT_FOUND_MASK;
import org.mule.extension.email.internal.EmailAttributes;
import org.mule.extension.email.internal.exception.EmailException;
import org.mule.runtime.core.util.IOUtils;

import java.io.InputStream;

import javax.activation.DataHandler;
import javax.mail.Address;
import javax.mail.Message;
import javax.mail.Multipart;
import javax.mail.internet.MimeMessage;

import org.junit.Test;
import org.mockito.internal.matchers.StartsWith;

public class EmailSenderTestCase extends EmailConnectorTestCase
{

    private static final String SEND_EMAIL = "sendEmail";
    private static final String SEND_EMAIL_WITH_ATTACHMENT = "sendEmailWithAttachment";
    private static final String FORWARD_EMAIL = "forwardEmail";
    private static final String REPLY_EMAIL = "replyEmail";

    @Override
    protected String getConfigFile()
    {
        return "smtp.xml";
    }

    @Override
    public String getProtocol()
    {
        return "smtp";
    }

    @Test
    public void sendEmail() throws Exception
    {
        runFlow(SEND_EMAIL);
        assertThat(server.waitForIncomingEmail(5000, 1), is(true));
        Message[] messages = server.getReceivedMessages();
        assertThat(messages.length, is(1));
        assertMessageSubjectAndContentText(messages[0]);
    }


    // TODO: This test fails if the attachment content-type is specified
    @Test
    public void sendEmailWithAttachment() throws Exception
    {
        runFlow(SEND_EMAIL_WITH_ATTACHMENT);
        assertThat(server.waitForIncomingEmail(5000, 4), is(true));
        Message[] messages = server.getReceivedMessages();
        assertThat(messages.length, is(4));

        for (Message message : messages)
        {
            Multipart content = (Multipart)message.getContent();
            assertThat(content.getCount(), is(2));

            Object bodyContent = content.getBodyPart(0).getContent();
            assertThat(bodyContent, is(CONTENT));

            DataHandler dataHandler = content.getBodyPart(1).getDataHandler();
            assertThat(dataHandler.getContent(), instanceOf(InputStream.class));
            assertThat(JSON_ATTACHMENT_CONTENT, is(IOUtils.toString((InputStream) dataHandler.getContent())));
        }
    }

    @Test
    public void replyEmail() throws Exception
    {
        EmailAttributes attributes = fromMessage(defaultMimeMessageBuilder(JUANI_EMAIL)
                                                              .replyTo(singletonList(MG_EMAIL))
                                                              .build());
        flowRunner(REPLY_EMAIL)
                .withPayload(CONTENT)
                .withAttributes(attributes)
                .run();

        assertThat(server.waitForIncomingEmail(5000, 1), is(true));
        MimeMessage[] messages = server.getReceivedMessages();
        assertThat(messages.length, is(1));
        assertThat(messages[0].getSubject(), new StartsWith("Re"));
        Address[] recipients = messages[0].getAllRecipients();
        assertThat(recipients.length, is(1));
        assertThat(recipients[0].toString(), is(MG_EMAIL));
    }

    @Test
    public void failReply() throws Exception
    {
        expectedException.expectCause(isA(EmailException.class));
        expectedException.expectMessage(containsString(format(ATTRIBUTES_NOT_FOUND_MASK, "")));
        runFlow(REPLY_EMAIL);
    }

    @Test
    public void forwardEmail() throws Exception
    {
        EmailAttributes attributes = fromMessage(defaultMimeMessageBuilder(JUANI_EMAIL)
                                                         .replyTo(singletonList(MG_EMAIL))
                                                         .build());
        flowRunner(FORWARD_EMAIL)
                .withPayload(CONTENT)
                .withAttributes(attributes)
                .run();

        assertThat(server.waitForIncomingEmail(5000, 1), is(true));
        MimeMessage[] messages = server.getReceivedMessages();
        assertThat(messages.length, is(1));
        assertThat(messages[0].getSubject(), new StartsWith("Fwd"));
        assertThat(messages[0].getContent().toString().trim(), is(CONTENT));
    }
}
