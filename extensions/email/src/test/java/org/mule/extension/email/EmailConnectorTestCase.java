package org.mule.extension.email;/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

import static java.util.Collections.singletonList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.mule.extension.email.internal.util.EmailUtils.getBody;
import org.mule.extension.email.api.EmailConnector;
import org.mule.extension.email.internal.builder.MessageBuilder;
import org.mule.functional.junit4.ExtensionFunctionalTestCase;
import org.mule.tck.junit4.rule.DynamicPort;

import com.icegreen.greenmail.user.GreenMailUser;
import com.icegreen.greenmail.util.GreenMail;
import com.icegreen.greenmail.util.ServerSetup;

import java.io.IOException;
import java.util.Properties;

import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Session;

import org.junit.Rule;
import org.junit.rules.ExpectedException;

public abstract class EmailConnectorTestCase extends ExtensionFunctionalTestCase
{
    @Rule
    public DynamicPort PORT = new DynamicPort("port");

    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    protected static final String SUBJECT = "Email Subject";
    protected static final String CONTENT = "Email Content";
    protected static final String JSON_ATTACHMENT_CONTENT =  "{\r\n  \"key\": \"value\"\r\n}";

    protected static final String PABLON_EMAIL = "pablo.musumeci@mulesoft.com";
    protected static final String ESTEBAN_EMAIL = "esteban.wasinger@mulesoft.com";
    protected static final String JUANI_EMAIL = "juan.desimoni@mulesoft.com";
    protected static final String ALE_EMAIL = "ale.g.marra@mulesoft.com";
    protected static final String MG_EMAIL = "mariano.gonzalez@mulesoft.com";
    protected static final String[] EMAILS = {JUANI_EMAIL, ESTEBAN_EMAIL, PABLON_EMAIL, ALE_EMAIL};

    protected static Session testSession = Session.getDefaultInstance(new Properties());

    protected GreenMail server;
    protected GreenMailUser user;

    @Override
    protected Class<?>[] getAnnotatedExtensionClasses()
    {
        return new Class<?>[] {EmailConnector.class};
    }

    @Override
    protected void doSetUpBeforeMuleContextCreation() throws Exception
    {
        ServerSetup serverSetup = new ServerSetup(PORT.getNumber(), null, getProtocol());
        server = new GreenMail(serverSetup);
        server.start();
        user = server.setUser(JUANI_EMAIL, JUANI_EMAIL, "password");
    }

    @Override
    protected void doTearDownAfterMuleContextDispose() throws Exception
    {
        server.stop();
    }

    protected void assertMessageSubjectAndContentText(Message message) throws MessagingException, IOException
    {
        assertThat(message.getSubject(), is(SUBJECT));
        assertThat(getBody(message), is(CONTENT));
    }

    public static MessageBuilder defaultMimeMessageBuilder(String to) throws MessagingException
    {
        return MessageBuilder.newMessage(testSession)
                .to(singletonList(to))
                .cc(singletonList(ALE_EMAIL))
                .withContent(CONTENT)
                .withSubject(SUBJECT);
    }


    public abstract String getProtocol();
}
