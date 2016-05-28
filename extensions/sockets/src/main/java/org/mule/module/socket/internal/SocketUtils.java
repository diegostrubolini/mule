/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.socket.internal;

import static org.apache.commons.lang3.StringUtils.isBlank;
import org.mule.module.socket.api.client.SocketClient;
import org.mule.module.socket.api.exceptions.UnresolvableHostException;
import org.mule.module.socket.api.source.SocketAttributes;
import org.mule.runtime.api.connection.ConnectionException;
import org.mule.runtime.api.connection.ConnectionExceptionCode;
import org.mule.runtime.api.connection.ConnectionValidationResult;
import org.mule.runtime.api.message.MuleMessage;
import org.mule.runtime.api.message.NullPayload;
import org.mule.runtime.api.metadata.DataType;
import org.mule.runtime.core.DefaultMuleMessage;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.serialization.ObjectSerializer;
import org.mule.runtime.core.transformer.types.DataTypeFactory;
import org.mule.runtime.core.util.IOUtils;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;

public class SocketUtils
{

    private static final int PORT_CHOSEN_BY_SYSTEM = 0;

    public static void write(OutputStream os, Object data, boolean payloadOnly, boolean streamingOk, ObjectSerializer objectSerializer) throws IOException
    {
        writeByteArray(os, getByteArray(data, payloadOnly, streamingOk, objectSerializer));
    }
    protected static void writeByteArray(OutputStream os, byte[] data) throws IOException
    {
        os.write(data);
    }

    public static byte[] getByteArray(Object data, boolean payloadOnly, boolean streamingOk, ObjectSerializer objectSerializer) throws IOException
    {
        if (data instanceof InputStream)
        {
            if (streamingOk)
            {
                return IOUtils.toByteArray((InputStream) data);
            }
            else
            {
                throw new IOException("Streaming is not allowed");
            }
        }
        else if (data instanceof MuleMessage)
        {
            if (payloadOnly)
            {
                return getByteArray(((MuleMessage) data).getPayload(), streamingOk, payloadOnly, objectSerializer);
            }
            else
            {
                return objectSerializer.serialize(data);
            }
        }
        else if (data instanceof byte[])
        {
            return (byte[]) data;
        }
        else if (data instanceof String)
        {
            return ((String) data).getBytes();
        }
        else if (data instanceof Serializable)
        {
            return objectSerializer.serialize(data);
        }

        throw new IllegalArgumentException(String.format("Cannot serialize data: '%s'", data));
    }

    public static ConnectionValidationResult validate(SocketClient client)
    {
        try
        {
            client.validate();
            return ConnectionValidationResult.success();
        }
        catch (UnresolvableHostException e)
        {
            return ConnectionValidationResult.failure(e.getMessage(), ConnectionExceptionCode.UNKNOWN_HOST, e);
        }
        catch (ConnectionException e)
        {
            return ConnectionValidationResult.failure(e.getMessage(), ConnectionExceptionCode.UNKNOWN, e);
        }
    }

    public static void closeSocket(DatagramSocket socket)
    {
        socket.close();
    }

    public static InetSocketAddress getAddress(String host, Integer port)
    {
        if (port == null && isBlank(host))
        {
            return new InetSocketAddress(PORT_CHOSEN_BY_SYSTEM);
        }
        else if (port == null && !isBlank(host))
        {
            return new InetSocketAddress(host, PORT_CHOSEN_BY_SYSTEM);
        }
        else if (port != null && isBlank(host))
        {
            return new InetSocketAddress(port);
        }

        return new InetSocketAddress(host, port);
    }

    public static InetSocketAddress getSocketAddress(String host, Integer port, boolean failOnUnresolvedHost)
    {

        InetSocketAddress address = getAddress(host, port);
        if (address.isUnresolved() && failOnUnresolvedHost)
        {
            throw new UnresolvableHostException(String.format("Host '%s' could not be resolved", host));
        }
        return address;
    }

    public static InetAddress getSocketAddressbyName(String host)
            throws UnresolvableHostException
    {
        try
        {
            return InetAddress.getByName(host);
        }
        catch (UnknownHostException e)
        {
            throw new UnresolvableHostException(String.format("Host name '%s' could not be resolved", host));
        }
    }

    public static MuleMessage<InputStream, SocketAttributes> createMuleMessage(InputStream content, SocketAttributes attributes, MuleContext muleContext)
    {
        DataType dataType = DataTypeFactory.create(InputStream.class);
        Object payload = NullPayload.getInstance();
        MuleMessage<InputStream, SocketAttributes> message;

        if (content != null)
        {
            payload = content;
        }

        message = (MuleMessage) new DefaultMuleMessage(payload, dataType, attributes, muleContext);
        return message;
    }

    public static MuleMessage<InputStream, SocketAttributes> createMuleMessageWithNullPayload(SocketAttributes attributes, MuleContext muleContext)
    {
        Object payload = NullPayload.getInstance();
        DataType dataType = DataTypeFactory.create(NullPayload.class);
        return (MuleMessage) new DefaultMuleMessage(payload, dataType, attributes, muleContext);
    }
}