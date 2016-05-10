/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.email.internal.util;

import static java.lang.String.format;
import static javax.mail.Part.ATTACHMENT;
import static org.mule.extension.email.internal.util.EmailConstants.MULTIPART;
import static org.mule.extension.email.internal.util.EmailConstants.TEXT;
import org.mule.extension.email.internal.EmailAttributes;
import org.mule.extension.email.internal.exception.EmailException;
import org.mule.runtime.api.message.MuleMessage;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.activation.DataHandler;
import javax.mail.Address;
import javax.mail.BodyPart;
import javax.mail.Multipart;
import javax.mail.Part;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;

/**
 * this class contains common methods for email handling.
 *
 * @since 4.0
 */
public class EmailUtils
{
    public static final String ATTRIBUTES_NOT_FOUND_MASK = "No email attributes were found in the incoming message. %s";

    /**
     * Converts a simple {@link String} representing an address into an {@link InternetAddress} instance
     *
     * @param address the string to be converted.
     * @return a new {@link InternetAddress} instance.
     */
    public static Address toAddress(String address)
    {
        try
        {
            return new InternetAddress(address);
        }
        catch (AddressException e)
        {
            throw new EmailException(format("Error while creating %s InternetAddress", address));
        }
    }

    /**
     * Converts a {@link List} of {@link String}s representing email addresses into an {@link InternetAddress} array.
     *
     * @param addresses the list to be converted.
     * @return a new {@link Address}[] instance.
     */
    public static Address[] toAddressArray(List<String> addresses)
    {
        return addresses.stream().map(EmailUtils::toAddress).toArray(Address[]::new);
    }

    /**
     * Extracts the incoming {@link MuleMessage} attributes of {@link EmailAttributes} type.
     * <p>
     * If no {@link EmailAttributes} are found in the incoming {@link MuleMessage}
     * an exception with the {@code exceptionMessage} is thrown.
     *
     * @param muleMessage      the incoming {@link MuleMessage}.
     * @param exceptionMessage i
     * @return an value with the {@link EmailAttributes}.
     */
    public static EmailAttributes getAttributesFromMessage(MuleMessage muleMessage, String exceptionMessage)
    {
        if (muleMessage.getAttributes() instanceof EmailAttributes)
        {
            return (EmailAttributes) muleMessage.getAttributes();
        }
        throw new EmailException(format(ATTRIBUTES_NOT_FOUND_MASK, exceptionMessage));
    }

    /**
     * Extracts the incoming {@link MuleMessage} attributes of {@link EmailAttributes} type.
     * <p>
     * If no {@link EmailAttributes} are found in the incoming {@link MuleMessage} an exception is thrown.
     *
     * @param muleMessage the incoming {@link MuleMessage}.
     * @return an value with the {@link EmailAttributes}.
     */
    public static EmailAttributes getAttributesFromMessage(MuleMessage muleMessage)
    {
        return getAttributesFromMessage(muleMessage, "");
    }

    /**
     * Extracts the text body of an email part.
     *
     * @param part the part to getPropertiesInstance the body from.
     * @return the body of the part.
     */
    public static String getBody(Part part)
    {
        try
        {
            Object content = part.getContent();
            if (part.isMimeType(TEXT))
            {
                return content.toString().trim();
            }
            else if (part.isMimeType(MULTIPART))
            {
                Multipart mp = (Multipart) content;
                for (int i = 0; i < mp.getCount(); i++)
                {
                    BodyPart bodyPart = mp.getBodyPart(i);
                    if (bodyPart.isMimeType(TEXT))
                    {
                        return bodyPart.getContent().toString().trim();
                    }
                }
            }
            return "";
        }
        catch (Exception e)
        {
            throw new EmailException(e.getMessage(), e);
        }
    }

    /**
     * Extracts the attachments of an email part.
     *
     * @param part the part to getPropertiesInstance the attachments from.
     * @return a {@link Map} with the email attachments.
     */
    public static Map<String, DataHandler> getAttachments(Part part)
    {
        Map<String, DataHandler> attachments = new HashMap<>();

        try
        {
            if (part.isMimeType(MULTIPART))
            {
                Multipart mp = (Multipart) part.getContent();
                for (int i = 0; i < mp.getCount(); i++)
                {
                    getAttachments(mp.getBodyPart(i));
                }
            }

            if (!part.isMimeType(TEXT) && part.getDisposition().equals(ATTACHMENT))
            {
                DataHandler dataHandler = part.getDataHandler();
                attachments.put(dataHandler.getName(), dataHandler);
            }
        }
        catch (Exception e)
        {
            throw new EmailException(e.getMessage(), e);
        }

        return attachments;
    }
}
