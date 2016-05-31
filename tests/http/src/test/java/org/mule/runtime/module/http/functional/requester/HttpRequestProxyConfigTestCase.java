/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.http.functional.requester;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import org.mule.extension.http.api.HttpConnector;
import org.mule.functional.junit4.ExtensionFunctionalTestCase;
import org.mule.module.socket.api.SocketsExtension;
import org.mule.runtime.core.api.MessagingException;
import org.mule.runtime.core.construct.Flow;
import org.mule.runtime.core.util.concurrent.Latch;
import org.mule.runtime.module.http.api.requester.proxy.ProxyConfig;
import org.mule.runtime.module.http.internal.request.DefaultHttpRequester;
import org.mule.runtime.module.http.internal.request.NtlmProxyConfig;
import org.mule.tck.junit4.rule.DynamicPort;

import java.io.IOException;
import java.net.ServerSocket;
import java.util.Arrays;
import java.util.Collection;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;

@RunWith(Parameterized.class)
public class HttpRequestProxyConfigTestCase extends ExtensionFunctionalTestCase
{

    private static final String PROXY_HOST = "localhost";
    private static final String PROXY_USERNAME = "theUsername";
    private static final String PROXY_PASSWORD = "thePassword";
    private static final String PROXY_NTLM_DOMAIN = "theNtlmDomain";

    @Rule
    public DynamicPort proxyPort = new DynamicPort("proxyPort");

    @Rule
    public DynamicPort httpPort = new DynamicPort("httpPort");

    private Thread mockProxyAcceptor;
    private Latch latch = new Latch();
    private Latch proxyReadyLatch = new Latch();

    @Parameter(0)
    public String flowName;

    @Parameter(1)
    public ProxyType proxyType;

    @Parameters(name = "{0}")
    public static Collection<Object[]> parameters()
    {
        return Arrays.asList(new Object[][] {
                {"refAnonymousProxy", ProxyType.ANONYMOUS},
                {"innerAnonymousProxy", ProxyType.ANONYMOUS},
                {"refUserPassProxy", ProxyType.USER_PASS},
                {"innerUserPassProxy", ProxyType.USER_PASS},
                {"refNtlmProxy", ProxyType.NTLM},
                {"innerNtlmProxy", ProxyType.NTLM}});
    }

    @Override
    protected String getConfigFile()
    {
        return "http-request-proxy-config.xml";
    }

    @Override
    protected Class<?>[] getAnnotatedExtensionClasses()
    {
        return new Class<?>[] {SocketsExtension.class, HttpConnector.class};
    }

    @Before
    public void startMockProxy() throws IOException, InterruptedException
    {
        mockProxyAcceptor = new MockProxy();
        mockProxyAcceptor.start();

        // Give time to the proxy thread to start up completely
        proxyReadyLatch.await();
        Thread.yield();
    }

    @After
    public void stopMockProxy() throws Exception
    {
        mockProxyAcceptor.join();
    }

    @Test
    public void testProxy() throws Exception
    {
        //TODO: Analyse if this check is still possible
        //checkProxyConfig((Flow) getFlowConstruct(flowName));
        ensureRequestGoesThroughProxy(flowName);
    }

    private void checkProxyConfig(Flow flow)
    {
        DefaultHttpRequester httpRequester = (DefaultHttpRequester) flow.getMessageProcessors().get(0);
        ProxyConfig proxyConfig = httpRequester.getConfig().getProxyConfig();

        assertThat(proxyConfig.getHost(), is(PROXY_HOST));
        assertThat(proxyConfig.getPort(), is(Integer.valueOf(proxyPort.getValue())));

        if (proxyType == ProxyType.USER_PASS || proxyType == ProxyType.NTLM)
        {
            assertThat(proxyConfig.getUsername(), is(PROXY_USERNAME));
            assertThat(proxyConfig.getPassword(), is(PROXY_PASSWORD));
            if (proxyType == ProxyType.NTLM)
            {
                assertThat(proxyConfig, is(instanceOf(NtlmProxyConfig.class)));
                assertThat(((NtlmProxyConfig) proxyConfig).getNtlmDomain(), is(PROXY_NTLM_DOMAIN));
            }
        }
    }

    private void ensureRequestGoesThroughProxy(String flowName) throws Exception
    {
        MessagingException e = flowRunner(flowName).withPayload(TEST_MESSAGE).runExpectingException();
        // Request should go through the proxy.
        assertThat(e.getCauseException(), is(instanceOf(IOException.class)));
        assertThat(e.getCauseException().getMessage(), is("Remotely closed"));
        latch.await(1, SECONDS);
    }

    private enum ProxyType
    {
        ANONYMOUS,
        USER_PASS,
        NTLM
    }

    private class MockProxy extends Thread
    {

        @Override
        public void run()
        {
            ServerSocket serverSocket = null;
            try
            {
                serverSocket = new ServerSocket(Integer.parseInt(proxyPort.getValue()));
                proxyReadyLatch.countDown();
                serverSocket.accept().close();
                latch.release();
            }
            catch (IOException e)
            { /* Ignore */ }
            finally
            {
                if (serverSocket != null)
                {
                    try
                    {
                        serverSocket.close();
                    }
                    catch (IOException e)
                    { /* Ignore */ }
                }
            }
        }
    }
}
