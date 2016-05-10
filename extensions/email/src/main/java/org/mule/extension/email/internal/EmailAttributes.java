/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.email.internal;

import org.mule.runtime.api.message.MuleMessage;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.activation.DataHandler;
import javax.mail.Folder;
import javax.mail.Message;

/**
 * Contains all the metadata of an email, it carries information such as
 * the subject of the email, the id in the mailbox and the recipitiens between others.
 * <p>
 * This class aims to be returned as attributes in a {@link MuleMessage} for every
 * retriever operation.
 * <p>
 * The attachments of the email are also carried in an {@link EmailAttributes} instance
 * and separated from the original multipart {@link Message}.
 *
 * @since 4.0
 */
public class EmailAttributes implements Serializable
{

    private int id;
    private List<String> fromAddresses = new ArrayList<>();
    private List<String> toAddresses = new ArrayList<>();
    private List<String> ccAddresses = new ArrayList<>();
    private List<String> bccAddresses = new ArrayList<>();
    private List<String> replyToAddresses = new ArrayList<>();
    private Map<String, String> headers = new HashMap<>();
    private String subject;
    private LocalDateTime receivedDate;
    private EmailFlags flags;
    private Map<String, DataHandler> attachments;

    /**
     * Creates a new instance.
     *
     * @param id the id of the email.
     * @param subject the subject of the email
     * @param fromAddresses the addresses that are sending the email.
     * @param toAddresses the primary addresses to deliver the email.
     * @param bccAddresses the blind carbon copy addresses to deliver the email.
     * @param ccAddresses the carbon copy addresses to deliver the email.
     * @param headers the header of the email.
     * @param attachments the attachments on the content of the email.
     * @param receivedDate the received date of the email.
     * @param flags the {@link EmailFlags} setted on the email.
     * @param replyToAddresses the addresses to reply to this message.
     */
    public EmailAttributes(int id,
                           String subject,
                           List<String> fromAddresses,
                           List<String> toAddresses,
                           List<String> bccAddresses,
                           List<String> ccAddresses,
                           Map<String, String> headers,
                           Map<String, DataHandler> attachments,
                           LocalDateTime receivedDate,
                           EmailFlags flags,
                           List<String> replyToAddresses)
    {
        this.id = id;
        this.attachments = attachments;
        this.bccAddresses = bccAddresses;
        this.ccAddresses = ccAddresses;
        this.fromAddresses = fromAddresses;
        this.headers = headers;
        this.receivedDate = receivedDate;
        this.replyToAddresses = replyToAddresses;
        this.subject = subject;
        this.flags = flags;
        this.toAddresses = toAddresses;
    }

    /**
     * Get the Message id of the email.
     * the id is the relative position of the email
     * in its Folder. Note that the id for a
     * particular email can change during a session
     * if other emails in the Folder are isDeleted and expunged.
     * <p>
     * Valid message ids start at 1. Emails that do not belong
     * to any folder (like newly composed or derived messages) have 0
     * as their message id.
     *
     * @return the message id
     */
    public int getId()
    {
        return id;
    }

    /**
     * Get the addresses to which replies should be directed.
     * This will usually be the sender of the email, but
     * some emails may direct replies to a different address
     *
     * @return all the recipient addresses of replyTo type.
     */
    public List<String> getReplyToAddresses()
    {
        return replyToAddresses;
    }

    /**
     * @return the subject of the email.
     */
    public String getSubject()
    {
        return subject;
    }

    /**
     * @return all the recipient addresses of "To" (primary) type.
     */
    public List<String> getToAddresses()
    {
        return toAddresses;
    }

    /**
     * @return all the recipient addresses of "Bcc" (blind carbon copy) type.
     */
    public List<String> getBccAddresses()
    {
        return bccAddresses;
    }

    /**
     * @return all the recipient addresses of "Cc" (carbon copy) type.
     */
    public List<String> getCcAddresses()
    {
        return ccAddresses;
    }

    /**
     * Get the identity of the person(s) who wished this message to
     * be sent.
     *
     * @return all the from addresses.
     */
    public List<String> getFromAddresses()
    {
        return fromAddresses;
    }

    /**
     * Get the date this message was received.
     * Different {@link Folder} implementations
     * may assign this value or not.
     *
     * @return the date this message was received.
     */
    public LocalDateTime getReceivedDate()
    {
        return receivedDate;
    }

    /**
     * @return all the headers of this email message.
     */
    public Map<String, String> getHeaders()
    {
        return headers;
    }

    /**
     * Get the attachments of the email, there are returned as a {@link Map}
     * name/content in which the name is represented with a {@link String} and
     * the content with a {@link DataHandler}.
     * <p>
     * If there is no attachments the an empty {@link Map} will be returned.
     * No attachments means that the content was not opened or the email message
     * has no attachments.
     *
     * @return a {@link Map} with the attachments of the email message.
     */
    public Map<String, DataHandler> getAttachments()
    {
        return attachments;
    }

    /**
     * @return an {@link EmailFlags} object containing the flags setted
     * in the email.
     */
    public EmailFlags getFlags()
    {
        return flags;
    }
}
