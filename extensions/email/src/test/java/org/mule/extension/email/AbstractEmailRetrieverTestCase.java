/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.extension.email;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import org.mule.runtime.api.message.MuleMessage;
import org.mule.runtime.core.api.MuleEvent;

import java.util.List;

import javax.mail.MessagingException;

import org.junit.Test;

public abstract class AbstractEmailRetrieverTestCase extends EmailConnectorTestCase
{

    public static final String RETRIEVE_AND_READ = "retrieveAndRead";
    public static final String RETRIEVE_WITH_ATTACHMENTS = "retrieveWithAttachments";
    public static final String RETRIEVE_AND_DONT_READ = "retrieveAndDontRead";
    public static final String RETRIEVE_AND_THEN_EXPUNGE_DELETE = "retrieveAndThenExpungeDelete";
    public static final String RETRIEVE_AND_MARK_AS_DELETE = "retrieveAndMarkDelete";
    public static final String RETRIEVE_AND_MARK_AS_READ = "retrieveAndMarkRead";

    @Test
    public void retrieveNothing() throws Exception
    {
        assertThat(server.getReceivedMessages().length, is(0));
        MuleEvent event = runFlow(RETRIEVE_AND_READ);
        List<MuleMessage> messages = (List<MuleMessage>) event.getMessage().getPayload();
        assertThat(messages, hasSize(0));
    }

    @Test
    public void retrieveEmailWithAttachments() throws Exception
    {
        // Test this.
    }

    protected void deliver10To(String email) throws MessagingException
    {
        for (int i = 0; i < 10; i++)
        {
            user.deliver(defaultMimeMessageBuilder(email).build());
        }
    }
}
