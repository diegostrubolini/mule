/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.email.api.retriever;

import static java.lang.String.format;
import static org.mule.extension.email.internal.EmailPropertiesFactory.getPropertiesInstance;
import static org.mule.runtime.api.connection.ConnectionExceptionCode.DISCONNECTED;
import static org.mule.runtime.api.connection.ConnectionValidationResult.failure;
import static org.mule.runtime.api.connection.ConnectionValidationResult.success;
import org.mule.extension.email.api.EmailConnection;
import org.mule.extension.email.internal.exception.EmailConnectionException;
import org.mule.extension.email.internal.exception.EmailException;
import org.mule.runtime.api.connection.ConnectionValidationResult;

import java.util.Map;
import java.util.Properties;

import javax.mail.Authenticator;
import javax.mail.Folder;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.Store;

/**
 * A connection with a mail server for retrieving emails from an specific folder.
 *
 * @since 4.0
 */
public class RetrieverConnection implements EmailConnection
{

    private final Session session;
    private final Store store;
    private final Folder folder;

    /**
     * Creates a new instance of the of the connection.
     *
     * @param protocol the desired protocol to use.
     * @param username the username to establish the connection.
     * @param password the password corresponding to the {@code username}.
     * @param host the host name of the mail server
     * @param port the port number of the mail server.
     * @param connectionTimeout the socket connection timeout
     * @param readTimeout the socket read timeout
     * @param writeTimeout the socket write timeout
     * @param properties additional custom properties.
     * @param authenticator custom authenticator to perform the connection with the mail server.
     * @param folder the folder to be opened in order to list emails.
     */
    public RetrieverConnection(String protocol,
                               String username,
                               String password,
                               String host,
                               String port,
                               long connectionTimeout,
                               long readTimeout,
                               long writeTimeout,
                               Map<String, String> properties,
                               Authenticator authenticator,
                               String folder) throws EmailConnectionException
    {
        Properties sessionProperties = getPropertiesInstance(protocol, host, port, connectionTimeout, readTimeout, writeTimeout, properties);
        this.session = Session.getInstance(sessionProperties, authenticator);
        try
        {
            this.store = session.getStore(protocol);
            this.store.connect(username, password);
            this.folder = store.getFolder(folder);
        }
        catch (MessagingException e)
        {
            throw new EmailConnectionException("Error while acquiring connection with the " + protocol + "store", e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public synchronized void disconnect()
    {
        try
        {
            if (folder.isOpen())
            {
                closeFolder(false);
            }
        }
        catch (Exception e)
        {
            throw new EmailException("Error while disconnecting", e);
        }
        finally
        {
            try
            {
                store.close();
            }
            catch (MessagingException e)
            {
                throw new EmailException("Closing the store associated to this connection", e);
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Session getSession()
    {
        return session;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ConnectionValidationResult validate()
    {
        String errorMessage = "Store is not connected";
        return store.isConnected() ? success()
                                   : failure(errorMessage, DISCONNECTED, new EmailConnectionException(errorMessage));
    }

    /**
     * opens and return the email {@link Folder} associated to this connection.
     * The folder can contain Messages, other Folders or both.
     *
     * @param mode open the folder READ_ONLY or READ_WRITE
     * @return the opened {@link Folder}
     */
    public Folder getOpenFolder(int mode)
    {
        try
        {
            if (!folder.isOpen())
            {
                folder.open(mode);
            }
            return folder;
        }
        catch (MessagingException e)
        {
            throw new EmailException(format("Error while opening folder [%s]", folder), e);
        }
    }

    /**
     * Closes the folder associated to this connection.
     *
     * @param expunge if should expunge the deleted messages from the mailbox or not.
     */
    public void closeFolder(boolean expunge)
    {
        try
        {
            folder.close(expunge);
        }
        catch (MessagingException e)
        {
            throw new EmailException(format("Error while closing the [%s] folder", folder.getName()), e);
        }
    }

}
