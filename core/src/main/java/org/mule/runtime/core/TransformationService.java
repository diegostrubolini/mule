/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.mule.runtime.core.util.ClassUtils.isConsumable;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.MuleEvent;
import org.mule.runtime.core.api.MuleException;
import org.mule.runtime.core.api.MuleMessage;
import org.mule.runtime.api.metadata.DataType;
import org.mule.runtime.core.api.transformer.Converter;
import org.mule.runtime.core.api.transformer.MessageTransformer;
import org.mule.runtime.core.api.transformer.Transformer;
import org.mule.runtime.core.api.transformer.TransformerException;
import org.mule.runtime.core.api.transformer.TransformerMessagingException;
import org.mule.runtime.core.config.i18n.CoreMessages;
import org.mule.runtime.core.transformer.TransformerUtils;
import org.mule.runtime.core.transformer.types.DataTypeFactory;
import org.mule.runtime.core.transformer.types.MimeTypes;

import java.io.InputStream;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Provides the same operations previously exposed by {@link MuleMessage} but decoupled from MuleMessage.
 *
 * TODO Redefine this interface as part of Mule 4.0 transformation improvements (MULE-9141)
 */
public class TransformationService
{

    private static final Log logger = LogFactory.getLog(TransformationService.class);

    private MuleContext muleContext;

    public TransformationService(MuleContext muleContext)
    {
        this.muleContext = muleContext;
    }

    /**
     * Applies a list of transformers returning the result of the transformation as a new message instance. If the list
     * of transformers is empty or transformation would be redundant then the same message instances will be returned.
     *
     * @param event the event being processed
     * @param transformers the transformers to apply to the message payload
     * @return the result of transformation
     * @throws TransformerException if a transformation error occurs or one or more of the transformers passed in a
     * are incompatible with the message payload
     */
    public MuleMessage applyTransformers(final MuleMessage message, final MuleEvent event, final List<? extends Transformer> transformers) throws MuleException
    {
        return applyAllTransformers(message, event, transformers);
    }

    /**
     * Applies a list of transformers returning the result of the transformation as a new message instance. If the list
     * of transformers is empty or transformation would be redundant then the same message instances will be returned.
     *
     * @param event the event being processed
     * @param transformers the transformers to apply to the message payload
     * @return the result of transformation
     * @throws TransformerException if a transformation error occurs or one or more of the transformers passed in a
     * are incompatible with the message payload
     */
    public MuleMessage applyTransformers(final MuleMessage message, final MuleEvent event, final Transformer... transformers) throws MuleException
    {
        return applyAllTransformers(message, event, Arrays.asList(transformers));
    }

    /**
     * Attempts to obtain the payload of this message with the desired Class type. This will
     * try and resolve a transformer that can do this transformation. If a transformer cannot be found
     * an exception is thrown.  Any transformers added to the registry will be checked for compatibility
     * <p/>
     * If the existing payload is consumable (i.e. can't be read twice) then the existing payload of the message will be
     * replaced with a byte[] representation as part of this operations.
     * <p/>
     *
     * @param outputDataType the desired return type
     * @return The converted payload of this message. Note that this method will not alter the payload of this
     * message *unless* the payload is an InputStream in which case the stream will be read and the payload will become
     * the fully read stream.
     * @throws TransformerException if a transformer cannot be found or there is an error during transformation of the
     * payload
     */
    public <T> MuleMessage transform(MuleMessage message, DataType<T> outputDataType) throws TransformerException
    {
        checkNotNull(message, "Message cannot be null");
        checkNotNull(outputDataType, "DataType cannot be null");
        return new DefaultMuleMessage(getPayload(message, outputDataType, message.getEncoding()), message, muleContext);
    }

    /**
     * Obtains a {@link String} representation of the message payload for logging without throwing exception.
     * <p/>
     * If the existing payload is consumable (i.e. can't be read twice) then the existing payload of the message will be
     * replaced with a byte[] representation as part of this operations.
     *
     * @return message payload as object
     */
    public String getPayloadForLogging(MuleMessage message)
    {
        return getPayloadForLogging(message, message.getEncoding());
    }

    /**
     * Obtains a {@link String} representation of the message payload for logging without throwing exception.
     * If encoding is required it will use the encoding set on the message.
     * <p>
     * If the existing payload is consumable (i.e. can't be read twice) or an exception occurs during transformation
     * then the an exeption won't be thrown but rather a description of the payload type will be returned.
     *
     * @return message payload as a String or message with the payload type if payload can't be converted to a String
     */
    public String getPayloadForLogging(MuleMessage message, String encoding)
    {
        Class type = message.getPayload().getClass();
        if (!isConsumable(type))
        {
            try
            {
                return getPayload(message, DataType.STRING_DATA_TYPE, encoding);
            }
            catch (TransformerException e)
            {
                return "Payload could not be converted to a String. Payload type is " + type;
            }
        }
        return "Payload is a stream of type: " + type;
    }

    private MuleMessage applyAllTransformers(final MuleMessage message, final MuleEvent event, final List<? extends Transformer> transformers) throws MuleException
    {
        MuleMessage result = message;
        if (!transformers.isEmpty())
        {
            for (int index = 0; index < transformers.size(); index++)
            {
                Transformer transformer = transformers.get(index);

                Class<?> srcCls = result.getPayload().getClass();
                DataType<?> originalSourceType = DataTypeFactory.create(srcCls);

                if (transformer.isSourceDataTypeSupported(originalSourceType))
                {
                    if (logger.isDebugEnabled())
                    {
                        logger.debug("Using " + transformer + " to transform payload.");
                    }
                    result = transformMessage(result, event, transformer);
                }
                else
                {
                    if (logger.isDebugEnabled())
                    {
                        logger.debug("Transformer " + transformer + " doesn't support the source payload: " + srcCls);
                    }

                    if (useExtendedTransformations())
                    {
                        if (canSkipTransformer(result, transformers, index))
                        {
                            continue;
                        }

                        // Resolves implicit conversion if possible
                        Transformer implicitTransformer = muleContext.getDataTypeConverterResolver().resolve(originalSourceType, transformer.getSourceDataTypes());

                        if (implicitTransformer != null)
                        {
                            if (logger.isDebugEnabled())
                            {
                                logger.debug("Performing implicit transformation with: " + transformer);
                            }
                            result = transformMessage(result, event, implicitTransformer);
                            result = transformMessage(result, event, transformer);
                        }
                        else
                        {
                            throw new IllegalArgumentException("Cannot apply transformer " + transformer + " on source payload: " + srcCls);
                        }
                    }
                    else if (!transformer.isIgnoreBadInput())
                    {
                        if (logger.isDebugEnabled())
                        {
                            logger.debug("Exiting from transformer chain (ignoreBadInput = false)");
                        }
                        break;
                    }
                }
            }
        }
        return result;
    }

    private boolean canSkipTransformer(MuleMessage message, List<? extends Transformer> transformers, int index)
    {
        Transformer transformer = transformers.get(index);

        boolean skipConverter = false;

        if (transformer instanceof Converter)
        {
            if (index == transformers.size() - 1)
            {
                try
                {
                    TransformerUtils.checkTransformerReturnClass(transformer, message.getPayload());
                    skipConverter = true;
                }
                catch (TransformerException e)
                {
                    // Converter cannot be skipped
                }
            }
            else
            {
                skipConverter= true;
            }
        }

        if (skipConverter)
        {
            logger.debug("Skipping converter: " + transformer);
        }

        return skipConverter;
    }

    private boolean useExtendedTransformations()
    {
        boolean result = true;
        if (muleContext != null && muleContext.getConfiguration() != null)
        {
            result = muleContext.getConfiguration().useExtendedTransformations();
        }

        return result;
    }

    private MuleMessage transformMessage(final MuleMessage message, final MuleEvent event, final Transformer transformer) throws TransformerMessagingException, TransformerException
    {
        Object result;

        if (transformer instanceof MessageTransformer)
        {
            result = ((MessageTransformer) transformer).transform(message, event);
        }
        else
        {
            result = transformer.transform(message);
        }

        if (result instanceof MuleMessage)
        {
            if (!result.equals(message))
            {
                // Only copy the payload and properties of mule message transformer result if the message is a different
                // instance
                MuleMessage transformResult = (MuleMessage) result;
                return new DefaultMuleMessage(result, transformResult, muleContext, transformResult.getDataType());
            }
            return  message;
        }
        else
        {
            return new DefaultMuleMessage(result, message, muleContext, mergeDataType(message, transformer.getReturnDataType(), result != null ? result.getClass() : null));
        }
    }

    private DataType<?> mergeDataType(MuleMessage message, DataType<?> transformed, Class<?> payloadTransformedClass)
    {
        DataType<?> original = message.getDataType();
        String mimeType = transformed.getMimeType() == null || MimeTypes.ANY.equals(transformed.getMimeType()) ? original.getMimeType() : transformed.getMimeType();
        String encoding = transformed.getEncoding() == null ? message.getEncoding() : transformed.getEncoding();
        // In case if the transformed dataType is an Object type we could keep the original type if it is compatible/assignable (String->Object we want to keep String as transformed DataType)
        Class<?> type = payloadTransformedClass != null && transformed.getType() == Object.class && original.isCompatibleWith(DataTypeFactory.create(payloadTransformedClass, mimeType)) ? original.getType() : transformed.getType();

        DataType mergedDataType = DataTypeFactory.create(type, mimeType);
        mergedDataType.setEncoding(encoding);
        return mergedDataType;
    }

    /**
     * Attempts to obtain the payload of this message with the desired Class type. This will
     * try and resolve a transformer that can do this transformation. If a transformer cannot be
     * found an exception is thrown. Any transformers added to the registry will be checked for
     * compatibility.
     *
     * @param resultType the desired return type
     * @param encoding   the encoding to use if required
     * @return The converted payload of this message. Note that this method will not alter the
     *         payload of this message <b>unless</b> the payload is an {@link InputStream} in which
     *         case the stream will be read and the payload will become the fully read stream.
     * @throws TransformerException if a transformer cannot be found or there is an error during
     *                              transformation of the payload.
     */
    @SuppressWarnings("unchecked")
    private  <T> T getPayload(MuleMessage message, DataType<T> resultType, String encoding) throws TransformerException
    {
        // Handle null by ignoring the request
        if (resultType == null)
        {
            throw new IllegalArgumentException(CoreMessages.objectIsNull("resultType").getMessage());
        }

        DataType source = DataTypeFactory.createFromObject(message);

        // If no conversion is necessary, just return the payload as-is
        if (resultType.isCompatibleWith(source))
        {
            return (T) message.getPayload();
        }

        // The transformer to execute on this message
        Transformer transformer = muleContext.getRegistry().lookupTransformer(source, resultType);
        if (transformer == null)
        {
            throw new TransformerException(CoreMessages.noTransformerFoundForMessage(source, resultType));
        }

        // Pass in the message itself
        Object result = transformer.transform(message, encoding);

        // Unless we disallow Object.class as a valid return type we need to do this extra check
        if (!resultType.getType().isAssignableFrom(result.getClass()))
        {
            throw new TransformerException(CoreMessages.transformOnObjectNotOfSpecifiedType(resultType, result));
        }

        return (T) result;
    }

}
