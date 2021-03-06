/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.config.ThreadingProfile;
import org.mule.runtime.core.api.context.notification.AsyncMessageNotificationListener;
import org.mule.runtime.core.api.context.notification.ClusterNodeNotificationListener;
import org.mule.runtime.core.api.context.notification.ConnectionNotificationListener;
import org.mule.runtime.core.api.context.notification.CustomNotificationListener;
import org.mule.runtime.core.api.context.notification.ExceptionNotificationListener;
import org.mule.runtime.core.api.context.notification.ManagementNotificationListener;
import org.mule.runtime.core.api.context.notification.MuleContextNotificationListener;
import org.mule.runtime.core.api.context.notification.PipelineMessageNotificationListener;
import org.mule.runtime.core.api.context.notification.RegistryNotificationListener;
import org.mule.runtime.core.api.context.notification.RoutingNotificationListener;
import org.mule.runtime.core.api.context.notification.SecurityNotificationListener;
import org.mule.runtime.core.api.context.notification.ServerNotification;
import org.mule.runtime.core.api.context.notification.ServerNotificationListener;
import org.mule.runtime.core.api.context.notification.TransactionNotificationListener;
import org.mule.runtime.core.config.DefaultMuleConfiguration;
import org.mule.runtime.core.context.DefaultMuleContextBuilder;
import org.mule.runtime.core.context.notification.AsyncMessageNotification;
import org.mule.runtime.core.context.notification.ClusterNodeNotification;
import org.mule.runtime.core.context.notification.ConnectionNotification;
import org.mule.runtime.core.context.notification.CustomNotification;
import org.mule.runtime.core.context.notification.ExceptionNotification;
import org.mule.runtime.core.context.notification.ManagementNotification;
import org.mule.runtime.core.context.notification.MuleContextNotification;
import org.mule.runtime.core.context.notification.PipelineMessageNotification;
import org.mule.runtime.core.context.notification.RegistryNotification;
import org.mule.runtime.core.context.notification.RoutingNotification;
import org.mule.runtime.core.context.notification.SecurityNotification;
import org.mule.runtime.core.context.notification.ServerNotificationManager;
import org.mule.runtime.core.context.notification.TransactionNotification;
import org.mule.runtime.core.lifecycle.MuleContextLifecycleManager;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.runtime.core.work.MuleWorkManager;

import java.util.Map;
import java.util.Set;

import org.junit.Test;

public class DefaultMuleContextBuilderTestCase extends AbstractMuleTestCase
{
    @Test
    public void testBuildMuleContextDefault()
    {
        // Build
        DefaultMuleContextBuilder builder = new DefaultMuleContextBuilder();
        MuleContext muleContext = builder.buildMuleContext();

        // Assert
        assertNotNull(muleContext);
        assertEquals(DefaultMuleConfiguration.class, muleContext.getConfiguration().getClass());
        assertEquals(MuleContextLifecycleManager.class, muleContext.getLifecycleManager().getClass());
        assertEquals(ServerNotificationManager.class, muleContext.getNotificationManager().getClass());
        assertEquals(MuleWorkManager.class, muleContext.getWorkManager().getClass());
    }

    @Test
    public void testBuildMuleContextCustom()
    {
        // Build
        MuleContext muleContext = build();

        // Assert
        assertNotNull(muleContext);
        assertEquals(MyMuleConfiguration.class, muleContext.getConfiguration().getClass());
        assertEquals(MyLifeCycleManager.class, muleContext.getLifecycleManager().getClass());
        assertEquals(MyServerNotificationManager.class, muleContext.getNotificationManager().getClass());
        assertEquals(MyWorkManager.class, muleContext.getWorkManager().getClass());
    }

    /**
     * <p>After the mule context is built it has to contain the proper notification interfaces in the notification
     * manager</p>
     */
    @Test
    public void notificationManagerContainsTheCorrectInterfaces()
    {
        DefaultMuleContextBuilder builder = new DefaultMuleContextBuilder();
        builder.setMuleConfiguration(new MyMuleConfiguration());
        builder.setLifecycleManager(new MyLifeCycleManager());
        builder.setWorkManager(new MyWorkManager(ThreadingProfile.DEFAULT_THREADING_PROFILE, "test"));
        MuleContext muleContext = builder.buildMuleContext();

        Map<Class<? extends ServerNotificationListener>,Set<Class<? extends ServerNotification>>> interfaces =
                muleContext.getNotificationManager().getInterfaceToTypes();

        assertEquals(MuleContextNotification.class, interfaces.get(MuleContextNotificationListener.class).toArray()[0]);
        assertEquals(RoutingNotification.class, interfaces.get(RoutingNotificationListener.class).toArray()[0]);
        assertEquals(SecurityNotification.class, interfaces.get(SecurityNotificationListener.class).toArray()[0]);
        assertEquals(ManagementNotification.class, interfaces.get(ManagementNotificationListener.class).toArray()[0]);
        assertEquals(CustomNotification.class, interfaces.get(CustomNotificationListener.class).toArray()[0]);
        assertEquals(ConnectionNotification.class, interfaces.get(ConnectionNotificationListener.class).toArray()[0]);
        assertEquals(RegistryNotification.class, interfaces.get(RegistryNotificationListener.class).toArray()[0]);
        assertEquals(ExceptionNotification.class, interfaces.get(ExceptionNotificationListener.class).toArray()[0]);
        assertEquals(TransactionNotification.class, interfaces.get(TransactionNotificationListener.class).toArray()[0]);
        assertEquals(PipelineMessageNotification.class, interfaces.get(PipelineMessageNotificationListener.class).toArray()[0]);
        assertEquals(AsyncMessageNotification.class, interfaces.get(AsyncMessageNotificationListener.class).toArray()[0]);
        assertEquals(ClusterNodeNotification.class, interfaces.get(ClusterNodeNotificationListener.class).toArray()[0]);


    }

    private MuleContext build() {
        DefaultMuleContextBuilder builder = new DefaultMuleContextBuilder();
        builder.setMuleConfiguration(new MyMuleConfiguration());
        builder.setLifecycleManager(new MyLifeCycleManager());
        builder.setNotificationManager(new MyServerNotificationManager());
        builder.setWorkManager(new MyWorkManager(ThreadingProfile.DEFAULT_THREADING_PROFILE, "test"));
        return builder.buildMuleContext();
    }

    /**
     * Override, we don't want a {@link MuleContext} created for this test case.
     */
    protected MuleContext createMuleContext() throws Exception
    {
        return null;
    }

    static class MyLifeCycleManager extends MuleContextLifecycleManager
    {
        // subclass just for testing
    }

    static class MyMuleConfiguration extends DefaultMuleConfiguration
    {
        // subclass just for testing
    }

    static class MyServerNotificationManager extends ServerNotificationManager
    {
        // subclass just for testing
    }

    static class MyWorkManager extends MuleWorkManager
    {
        public MyWorkManager(ThreadingProfile profile, String name)
        {
            super(profile, name, 5000);
        }
    }
}
