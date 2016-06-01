/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.socket.api.source;

import static org.apache.commons.lang.StringUtils.EMPTY;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.Socket;

public class ImmutableSocketAttributes implements SocketAttributes
{

    private int remotePort;
    private String remoteHostAddress;
    private String remoteHostName;

    public ImmutableSocketAttributes(Socket socket)
    {
        fromInetAddress(socket.getPort(), socket.getInetAddress());
    }

    public ImmutableSocketAttributes(DatagramSocket datagramSocket)
    {
        fromInetAddress(datagramSocket.getPort(), datagramSocket.getInetAddress());
    }

    public ImmutableSocketAttributes(DatagramPacket packet)
    {
        this(packet.getPort(), packet.getAddress().getHostAddress(), packet.getAddress().getHostName());
    }

    private void fromInetAddress(int port, InetAddress address)
    {
        this.remotePort = port;
        if (address == null)
        {
            this.remoteHostName = EMPTY;
            this.remoteHostAddress = EMPTY;
        }
        else
        {
            this.remoteHostName = address.getHostName();
            this.remoteHostAddress = address.getHostAddress();
        }
    }


    public ImmutableSocketAttributes(int remotePort, String remoteHostAddress, String remoteHostName)
    {
        this.remotePort = remotePort;
        this.remoteHostAddress = remoteHostAddress;
        this.remoteHostName = remoteHostName;
    }

    public int getRemotePort()
    {
        return remotePort;
    }

    public String getRemoteHostAddress()
    {
        return remoteHostAddress;
    }

    public String getRemoteHostName()
    {
        return remoteHostName;
    }
}
