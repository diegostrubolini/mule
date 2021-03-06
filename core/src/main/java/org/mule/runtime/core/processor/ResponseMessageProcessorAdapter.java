/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.processor;

import org.mule.runtime.core.api.MuleEvent;
import org.mule.runtime.core.api.MuleException;
import org.mule.runtime.core.api.construct.FlowConstruct;
import org.mule.runtime.core.api.construct.FlowConstructAware;
import org.mule.runtime.core.api.context.MuleContextAware;
import org.mule.runtime.core.api.lifecycle.Disposable;
import org.mule.runtime.core.api.lifecycle.Initialisable;
import org.mule.runtime.core.api.lifecycle.InitialisationException;
import org.mule.runtime.core.api.lifecycle.Lifecycle;
import org.mule.runtime.core.api.lifecycle.Startable;
import org.mule.runtime.core.api.lifecycle.Stoppable;
import org.mule.runtime.core.api.processor.MessageProcessor;
import org.mule.runtime.core.execution.MessageProcessorExecutionTemplate;

import java.util.Collections;
import java.util.List;

public class ResponseMessageProcessorAdapter extends AbstractRequestResponseMessageProcessor implements Lifecycle,
        FlowConstructAware
{

    protected MessageProcessor responseProcessor;
    protected FlowConstruct flowConstruct;

    public ResponseMessageProcessorAdapter()
    {
        super();
    }

    public ResponseMessageProcessorAdapter(MessageProcessor responseProcessor)
    {
        super();
        this.responseProcessor = responseProcessor;
    }

    public void setProcessor(MessageProcessor processor)
    {
        this.responseProcessor = processor;
    }

    @Override
    protected MuleEvent processResponse(MuleEvent response, final MuleEvent request) throws MuleException
    {
        if (responseProcessor == null || !isEventValid(response))
        {
            return response;
        }
        else
        {
            return new CopyOnNullNonBlockingProcessorExecutor(response, Collections.singletonList(responseProcessor),
                                                              MessageProcessorExecutionTemplate
                                                                      .createExecutionTemplate(), true).execute();
        }
    }

    class CopyOnNullNonBlockingProcessorExecutor extends NonBlockingProcessorExecutor
    {

        public CopyOnNullNonBlockingProcessorExecutor(MuleEvent event, List<MessageProcessor> processors,
                                                      MessageProcessorExecutionTemplate executionTemplate, boolean
                copyOnVoidEvent)
        {
            super(event, processors, executionTemplate, copyOnVoidEvent);
        }

        @Override
        protected boolean isUseEventCopy(MuleEvent result)
        {
            return super.isUseEventCopy(result) || result == null;
        }
    }

    @Override
    public void initialise() throws InitialisationException
    {
        if (responseProcessor instanceof MuleContextAware)
        {
            ((MuleContextAware) responseProcessor).setMuleContext(muleContext);
        }
        if (responseProcessor instanceof FlowConstructAware)
        {
            ((FlowConstructAware) responseProcessor).setFlowConstruct(flowConstruct);
        }
        if (responseProcessor instanceof Initialisable)
        {
            ((Initialisable) responseProcessor).initialise();
        }
    }

    @Override
    public void start() throws MuleException
    {
        if (responseProcessor instanceof Startable)
        {
            ((Startable) responseProcessor).start();
        }
    }

    @Override
    public void stop() throws MuleException
    {
        if (responseProcessor instanceof Stoppable)
        {
            ((Stoppable) responseProcessor).stop();
        }
    }

    @Override
    public void dispose()
    {
        if (responseProcessor instanceof Disposable)
        {
            ((Disposable) responseProcessor).dispose();
        }
    }

    @Override
    public void setFlowConstruct(FlowConstruct flowConstruct)
    {
        this.flowConstruct = flowConstruct;
    }

}
