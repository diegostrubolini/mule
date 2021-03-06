/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.cxf;

import org.mule.runtime.core.NonBlockingVoidMuleEvent;
import org.mule.runtime.core.VoidMuleEvent;
import org.mule.runtime.core.api.MuleEvent;
import org.mule.runtime.core.api.MuleException;
import org.mule.runtime.core.api.MuleMessage;
import org.mule.runtime.core.api.config.MuleProperties;
import org.mule.runtime.core.api.execution.ExecutionCallback;
import org.mule.runtime.api.message.NullPayload;
import org.mule.runtime.core.component.ComponentException;
import org.mule.runtime.core.config.ExceptionHelper;
import org.mule.runtime.core.execution.ErrorHandlingExecutionTemplate;
import org.mule.runtime.core.transformer.types.DataTypeFactory;

import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.List;

import javax.script.ScriptException;

import org.apache.cxf.common.util.PropertyUtils;
import org.apache.cxf.frontend.MethodDispatcher;
import org.apache.cxf.helpers.CastUtils;
import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.message.Exchange;
import org.apache.cxf.message.FaultMode;
import org.apache.cxf.message.Message;
import org.apache.cxf.message.MessageContentsList;
import org.apache.cxf.service.Service;
import org.apache.cxf.service.invoker.Invoker;
import org.apache.cxf.service.model.BindingOperationInfo;

/**
 * Invokes a Mule Service via a CXF binding.
 */
public class MuleInvoker implements Invoker
{
    private final CxfInboundMessageProcessor cxfMmessageProcessor;
    private Class<?> targetClass;
    
    public MuleInvoker(CxfInboundMessageProcessor cxfMmessageProcessor, Class<?> targetClass)
    {
        this.cxfMmessageProcessor = cxfMmessageProcessor;
        this.targetClass = targetClass;
    }

    public Object invoke(Exchange exchange, Object o)
    {
        // this is the original request. Keep it to copy all the message properties from it
        final MuleEvent event = (MuleEvent) exchange.get(CxfConstants.MULE_EVENT);
        MuleEvent responseEvent = null;

        if (PropertyUtils.isTrue(exchange.remove(CxfConstants.NON_BLOCKING_RESPONSE)))
        {
            responseEvent = event;
        }
        else
        {
            try
            {
                MuleMessage reqMsg = event.getMessage();
                Object payload = extractPayload(exchange.getInMessage());
                Class payloadClass = payload != null ? payload.getClass() : Object.class;
                reqMsg.setPayload(payload, DataTypeFactory.create(payloadClass, cxfMmessageProcessor.getMimeType()));

                BindingOperationInfo bop = exchange.get(BindingOperationInfo.class);
                Service svc = exchange.get(Service.class);
                if (!cxfMmessageProcessor.isProxy())
                {
                    MethodDispatcher md = (MethodDispatcher) svc.get(MethodDispatcher.class.getName());
                    Method m = md.getMethod(bop);
                    if (targetClass != null)
                    {
                        m = matchMethod(m, targetClass);
                    }

                    event.setFlowVariable(MuleProperties.MULE_METHOD_PROPERTY, m);
                }

                if (bop != null)
                {
                    event.setFlowVariable(CxfConstants.INBOUND_OPERATION, bop.getOperationInfo().getName());
                    event.setFlowVariable(CxfConstants.INBOUND_SERVICE, svc.getName());
                }

                ErrorHandlingExecutionTemplate errorHandlingExecutionTemplate = ErrorHandlingExecutionTemplate.createErrorHandlingExecutionTemplate(event.getMuleContext(), event.getFlowConstruct().getExceptionListener());
                responseEvent = errorHandlingExecutionTemplate.execute(new ExecutionCallback<MuleEvent>()
                {
                    @Override
                    public MuleEvent process() throws Exception
                    {
                        return cxfMmessageProcessor.processNext(event);
                    }
                });
            }
            catch (MuleException e)
            {
                exchange.put(CxfConstants.MULE_EVENT, event);

                Throwable cause = e;

                // See MULE-6329
                if(Boolean.valueOf((String) event.getFlowVariable(CxfConstants.UNWRAP_MULE_EXCEPTIONS)))
                {
                    cause = ExceptionHelper.getNonMuleException(e);
                    // Exceptions thrown from a ScriptComponent or a ScriptTransformer are going to be wrapped on a
                    // ScriptException
                    if(cause instanceof ScriptException && cause.getCause() != null)
                    {
                        cause = cause.getCause();
                    }
                }
                else if(e instanceof ComponentException) {
                    cause = e.getCause();
                }

                throw new Fault(cause);
            }
            catch (Exception e)
            {
                exchange.put(CxfConstants.MULE_EVENT, event);
                throw new Fault(e);
            }

            if (!event.getExchangePattern().hasResponse())
            {
                // weird response from AbstractInterceptingMessageProcessor
                responseEvent = null;
            }

            if (responseEvent instanceof NonBlockingVoidMuleEvent)
            {
                exchange.put(Message.SUSPENDED_INVOCATION, true);
                exchange.put(CxfConstants.MULE_EVENT, responseEvent);
                return null;
            }

        }

        if (responseEvent != null && !VoidMuleEvent.getInstance().equals(responseEvent))
        {
            exchange.put(CxfConstants.MULE_EVENT, responseEvent);
            MuleMessage resMessage = responseEvent.getMessage();

            if (resMessage.getExceptionPayload() != null)
            {
                Throwable cause = resMessage.getExceptionPayload().getException();
                if (cause instanceof ComponentException)
                {
                    cause = cause.getCause();
                }

                exchange.getInMessage().put(FaultMode.class, FaultMode.UNCHECKED_APPLICATION_FAULT);
                if (cause instanceof Fault)
                {
                    throw (Fault) cause;
                }

                throw new Fault(cause);
            }
            else if (resMessage.getPayload() instanceof NullPayload)
            {
                return new MessageContentsList((Object)null);
            }
            else if (cxfMmessageProcessor.isProxy())
            {
                resMessage.getPayload();
                return new Object[] { resMessage };
            }
            else
            {
                return new Object[]{resMessage.getPayload()};
            }
        }
        else
        {
            exchange.getInMessage().getInterceptorChain().abort();
            if (exchange.getOutMessage() != null)
            {
                exchange.getOutMessage().getInterceptorChain().abort();
            }
            exchange.put(CxfConstants.MULE_EVENT, null);
            return null;
        }
    }

    protected Object extractPayload(Message cxfMessage)
    {   
        List<Object> list = CastUtils.cast(cxfMessage.getContent(List.class));
        if (list == null)
        {
            // Seems Providers get objects stored this way
            Object object = cxfMessage.getContent(Object.class);
            if (object != null)
            {
                return object;
            }
            else
            {
                return new Object[0];
            }
        }
        
        if ((list.size() == 1) && (list.get(0) != null))
        {
            return list.get(0);
        }
        else
        {
            return list.toArray();
        }
    }

    /**
     * Returns a Method that has the same declaring class as the class of
     * targetObject to avoid the IllegalArgumentException when invoking the
     * method on the target object. The methodToMatch will be returned if the
     * targetObject doesn't have a similar method.
     * 
     * @param methodToMatch The method to be used when finding a matching method
     *            in targetObject
     * @param targetClass The class to search in for the method.
     * @return The methodToMatch if no such method exist in the class of
     *         targetObject; otherwise, a method from the class of targetObject
     *         matching the matchToMethod method.
     */
    private static Method matchMethod(Method methodToMatch, Class<?> targetClass) 
    {
        Class<?>[] interfaces = targetClass.getInterfaces();
        for (int i = 0; i < interfaces.length; i++) 
        {
            Method m = getMostSpecificMethod(methodToMatch, interfaces[i]);
            if (!methodToMatch.equals(m)) 
            {
                return m;
            }
        }
        return methodToMatch;
    }

    /**
     * Return whether the given object is a J2SE dynamic proxy.
     * 
     * @param object the object to check
     * @see java.lang.reflect.Proxy#isProxyClass
     */
    public static boolean isJdkDynamicProxy(Object object) 
    {
        return object != null && Proxy.isProxyClass(object.getClass());
    }

    /**
     * Given a method, which may come from an interface, and a targetClass used
     * in the current AOP invocation, find the most specific method if there is
     * one. E.g. the method may be IFoo.bar() and the target class may be
     * DefaultFoo. In this case, the method may be DefaultFoo.bar(). This
     * enables attributes on that method to be found.
     * 
     * @param method method to be invoked, which may come from an interface
     * @param targetClass target class for the curren invocation. May be
     *            <code>null</code> or may not even implement the method.
     * @return the more specific method, or the original method if the
     *         targetClass doesn't specialize it or implement it or is null
     */
    public static Method getMostSpecificMethod(Method method, Class<?> targetClass) {
        if (method != null && targetClass != null) {
            try {
                method = targetClass.getMethod(method.getName(), method.getParameterTypes());
            } catch (NoSuchMethodException ex) {
                // Perhaps the target class doesn't implement this method:
                // that's fine, just use the original method
            }
        }
        return method;
    }
}
