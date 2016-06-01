/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.socket.api.protocol;

import org.mule.module.socket.api.exceptions.LengthExceededException;
import org.mule.runtime.extension.api.annotation.Parameter;
import org.mule.runtime.extension.api.annotation.param.Optional;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * The LengthProtocol is an application level tcp protocol that can be used to
 * transfer large amounts of data without risking some data to be loss. The protocol
 * is defined by sending / reading an integer (the packet length) and then the data
 * to be transferred.
 * <p>
 * <p>Note that use of this protocol must be symmetric - both the sending and receiving
 * connectors must use the same protocol.</p>
 *
 * @since 4.0
 */
public class LengthProtocol extends DirectProtocol
{

    private static final Log LOGGER = LogFactory.getLog(LengthProtocol.class);
    private static final int SIZE_INT = Integer.BYTES;

    @Parameter
    @Optional(defaultValue = "-1")
    private Integer maxMessageLength = NO_MAX_LENGTH;

    public LengthProtocol()
    {
        this(NO_MAX_LENGTH);
    }

    public LengthProtocol(int maxMessageLength)
    {
        super(NO_STREAM, SIZE_INT);
        this.maxMessageLength = maxMessageLength;
    }

    public InputStream read(InputStream socketIs) throws IOException
    {
        return new ByteArrayProtocolWrapper(socketIs, this::consume);
    }

    private byte[] consume(InputStream is) throws IOException
    {
        // original comments indicated that we need to use read(byte[]) rather than readInt()
        // to avoid socket timeouts - don't understand, but don't want to risk change.

        // first read the data necessary to know the length of the payload
        DataInputStream dis = new DataInputStream(is);
        dis.mark(SIZE_INT);
        // this pulls through SIZE_INT bytes
        if (null == super.read(dis, SIZE_INT))
        {
            return null; // eof
        }

        // reset and read the integer
        dis.reset();
        int length = dis.readInt();

        if (length < 0 || (maxMessageLength > 0 && length > maxMessageLength))
        {
            throw new LengthExceededException("Length " + length + " exceeds limit: " + maxMessageLength.toString());
        }

        // finally read the rest of the data
        byte[] buffer = new byte[length];
        dis.readFully(buffer);

        return buffer;
    }

    @Override
    protected void writeByteArray(OutputStream os, byte[] data) throws IOException
    {
        if (maxMessageLength > 0 && data.length > maxMessageLength)
        {
            throw new LengthExceededException(String.format("Message length is '%d' and exceeds the limit '%d", data.length, maxMessageLength));
        }
        // Write the length and then the data.
        DataOutputStream dos = new DataOutputStream(os);
        dos.writeInt(data.length);
        dos.write(data);
        // DataOutputStream size is SIZE_INT + the byte length, due to the writeInt call
        // this should fix EE-1494
        if (dos.size() != data.length + SIZE_INT)
        {
            // only flush if the sizes don't match up
            dos.flush();
        }
    }

    /**
     * Read all four bytes for initial integer (limit is set in read)
     *
     * @param len       Amount transferred last call (-1 on EOF or socket error)
     * @param available Amount available
     * @return true if the transfer should continue
     */
    @Override
    protected boolean isRepeat(int len, int available)
    {
        return true;
    }
}
