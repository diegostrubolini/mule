/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.extension.internal.introspection.describer;

import static org.apache.commons.lang.StringUtils.EMPTY;
import static org.mule.module.extension.internal.introspection.describer.MuleExtensionAnnotationParser.getExtension;
import static org.mule.module.extension.internal.introspection.describer.MuleExtensionAnnotationParser.getMemberName;
import static org.mule.module.extension.internal.introspection.describer.MuleExtensionAnnotationParser.parseDisplayAnnotations;
import static org.mule.module.extension.internal.util.IntrospectionUtils.getExposedFields;
import static org.mule.module.extension.internal.util.IntrospectionUtils.getField;
import static org.mule.module.extension.internal.util.IntrospectionUtils.getInterfaceGenerics;
import static org.mule.module.extension.internal.util.IntrospectionUtils.getOperationMethods;
import static org.mule.module.extension.internal.util.IntrospectionUtils.getParameterFields;
import static org.mule.module.extension.internal.util.IntrospectionUtils.getParameterGroupFields;
import static org.mule.module.extension.internal.util.IntrospectionUtils.getSourceName;
import static org.mule.module.extension.internal.util.IntrospectionUtils.getSuperClassGenerics;
import static org.mule.util.Preconditions.checkArgument;
import org.mule.api.connection.ConnectionProvider;
import org.mule.extension.api.annotation.Alias;
import org.mule.extension.api.annotation.Configuration;
import org.mule.extension.api.annotation.Configurations;
import org.mule.extension.api.annotation.Extensible;
import org.mule.extension.api.annotation.Extension;
import org.mule.extension.api.annotation.ExtensionOf;
import org.mule.extension.api.annotation.OnException;
import org.mule.extension.api.annotation.Operations;
import org.mule.extension.api.annotation.Sources;
import org.mule.extension.api.annotation.connector.Providers;
import org.mule.extension.api.annotation.param.Connection;
import org.mule.extension.api.annotation.param.Optional;
import org.mule.extension.api.annotation.param.UseConfig;
import org.mule.extension.api.annotation.param.display.Placement;
import org.mule.extension.api.exception.IllegalModelDefinitionException;
import org.mule.extension.api.introspection.DataType;
import org.mule.extension.api.introspection.ExceptionEnricherFactory;
import org.mule.extension.api.introspection.declaration.DescribingContext;
import org.mule.extension.api.introspection.declaration.fluent.ConfigurationDescriptor;
import org.mule.extension.api.introspection.declaration.fluent.ConnectionProviderDescriptor;
import org.mule.extension.api.introspection.declaration.fluent.DeclarationDescriptor;
import org.mule.extension.api.introspection.declaration.fluent.Descriptor;
import org.mule.extension.api.introspection.declaration.fluent.HasModelProperties;
import org.mule.extension.api.introspection.declaration.fluent.OperationDescriptor;
import org.mule.extension.api.introspection.declaration.fluent.ParameterDeclaration;
import org.mule.extension.api.introspection.declaration.fluent.ParameterDescriptor;
import org.mule.extension.api.introspection.declaration.fluent.SourceDescriptor;
import org.mule.extension.api.introspection.declaration.fluent.WithParameters;
import org.mule.extension.api.introspection.declaration.spi.Describer;
import org.mule.extension.api.introspection.property.display.ImmutablePlacementModelProperty;
import org.mule.extension.api.introspection.property.display.PlacementModelProperty;
import org.mule.extension.api.runtime.source.Source;
import org.mule.module.extension.internal.exception.IllegalConfigurationModelDefinitionException;
import org.mule.module.extension.internal.exception.IllegalConnectionProviderModelDefinitionException;
import org.mule.module.extension.internal.exception.IllegalOperationModelDefinitionException;
import org.mule.module.extension.internal.exception.IllegalParameterModelDefinitionException;
import org.mule.module.extension.internal.introspection.ParameterGroup;
import org.mule.module.extension.internal.introspection.VersionResolver;
import org.mule.module.extension.internal.model.property.ConfigTypeModelProperty;
import org.mule.module.extension.internal.model.property.ConnectionTypeModelProperty;
import org.mule.module.extension.internal.model.property.ExtendingOperationModelProperty;
import org.mule.module.extension.internal.model.property.ImplementingMethodModelProperty;
import org.mule.module.extension.internal.model.property.ImplementingTypeModelProperty;
import org.mule.module.extension.internal.model.property.ParameterGroupModelProperty;
import org.mule.module.extension.internal.model.property.TypeRestrictionModelProperty;
import org.mule.module.extension.internal.runtime.exception.DefaultExceptionEnricherFactory;
import org.mule.module.extension.internal.runtime.executor.ReflectiveOperationExecutorFactory;
import org.mule.module.extension.internal.runtime.source.DefaultSourceFactory;
import org.mule.module.extension.internal.util.IntrospectionUtils;
import org.mule.util.ArrayUtils;
import org.mule.util.CollectionUtils;
import org.mule.util.collection.ImmutableSetCollector;

import com.google.common.collect.ImmutableList;

import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Implementation of {@link Describer} which generates a {@link Descriptor} by
 * scanning annotations on a type provided in the constructor
 *
 * @since 3.7.0
 */
public final class AnnotationsBasedDescriber implements Describer
{

    public static final String DEFAULT_CONNECTION_PROVIDER_NAME = "connection";
    public static final String CUSTOM_CONNECTION_PROVIDER_SUFFIX = "-" + DEFAULT_CONNECTION_PROVIDER_NAME;

    private final Class<?> extensionType;
    private final VersionResolver versionResolver;

    /**
     * An ordered {@link List} used to locate a {@link FieldDescriber} that can handle
     * an specific {@link Field}
     */
    private List<FieldDescriber> fieldDescribers;

    public AnnotationsBasedDescriber(Class<?> extensionType)
    {
        this(extensionType, new ManifestBasedVersionResolver(extensionType));
    }

    public AnnotationsBasedDescriber(Class<?> extensionType, VersionResolver versionResolver)
    {
        checkArgument(extensionType != null, String.format("describer %s does not specify an extension type", getClass().getName()));
        this.extensionType = extensionType;
        this.versionResolver = versionResolver;

        initialiseFieldDescribers();
    }

    private void initialiseFieldDescribers()
    {
        fieldDescribers = ImmutableList.of(new TlsContextFieldDescriber(), new DefaultFieldDescriber());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public final Descriptor describe(DescribingContext context)
    {
        Extension extension = getExtension(extensionType);
        DeclarationDescriptor declaration = context.getDeclarationDescriptor()
                .named(extension.name())
                .onVersion(getVersion(extension))
                .fromVendor(extension.vendor())
                .describedAs(extension.description())
                .withExceptionEnricherFactory(getExceptionEnricherFactory(extensionType))
                .withModelProperty(ImplementingTypeModelProperty.KEY, new ImplementingTypeModelProperty(extensionType));

        declareConfigurations(declaration, extensionType);
        declareOperations(declaration, extensionType);
        declareConnectionProviders(declaration, extensionType);
        declareMessageSources(declaration, extensionType);

        return declaration;
    }

    private String getVersion(Extension extension)
    {
        return versionResolver.resolveVersion(extension);
    }

    private void declareConfigurations(DeclarationDescriptor declaration, Class<?> extensionType)
    {
        Class<?>[] configurationClasses = getConfigurationClasses(extensionType);
        if (ArrayUtils.isEmpty(configurationClasses))
        {
            declareConfiguration(declaration, extensionType);
        }
        else
        {
            for (Class<?> configurationClass : configurationClasses)
            {
                declareConfiguration(declaration, configurationClass);
            }
        }
    }

    private Class<?>[] getConfigurationClasses(Class<?> extensionType)
    {
        Configurations configs = extensionType.getAnnotation(Configurations.class);
        return configs == null ? ArrayUtils.EMPTY_CLASS_ARRAY : configs.value();
    }

    private void declareMessageSources(DeclarationDescriptor declaration, Class<?> extensionType)
    {
        Sources sources = extensionType.getAnnotation(Sources.class);
        if (sources != null)
        {
            for (Class<? extends Source> declaringClass : sources.value())
            {
                declareMessageSource(declaration, declaringClass);
            }
        }
    }

    private void declareConfiguration(DeclarationDescriptor declaration, Class<?> configurationType)
    {
        checkConfigurationIsNotAnOperation(configurationType);
        ConfigurationDescriptor configuration;

        Configuration configurationAnnotation = configurationType.getAnnotation(Configuration.class);
        if (configurationAnnotation != null)
        {
            configuration = declaration.withConfig(configurationAnnotation.name()).describedAs(configurationAnnotation.description());
        }
        else
        {
            configuration = declaration.withConfig(Extension.DEFAULT_CONFIG_NAME).describedAs(Extension.DEFAULT_CONFIG_DESCRIPTION);
        }

        configuration.createdWith(new TypeAwareConfigurationFactory(configurationType))
                .withModelProperty(ImplementingTypeModelProperty.KEY, new ImplementingTypeModelProperty(configurationType));

        declareAnnotatedParameters(configurationType, configuration, configuration.with());
    }

    private void checkConfigurationIsNotAnOperation(Class<?> configurationType)
    {
        Class<?>[] operationClasses = getOperationClasses(extensionType);
        for (Class<?> operationClass : operationClasses)
        {
            if (configurationType.isAssignableFrom(operationClass) || operationClass.isAssignableFrom(configurationType))
            {
                throw new IllegalConfigurationModelDefinitionException(String.format("Configuration class '%s' cannot be the same class (nor a derivative) of any operation class '%s",
                                                                                     configurationType.getName(), operationClass.getName()));
            }
        }
    }

    private void checkOperationIsNotAnExtension(Class<?> operationType)
    {
        if (operationType.isAssignableFrom(extensionType) || extensionType.isAssignableFrom(operationType))
        {
            throw new IllegalOperationModelDefinitionException(String.format("Operation class '%s' cannot be the same class (nor a derivative) of the extension class '%s",
                                                                             operationType.getName(), extensionType.getName()));
        }
    }

    private void declareMessageSource(DeclarationDescriptor declaration, Class<? extends Source> sourceType)
    {
        //TODO: MULE-9220: Add a Syntax validator which checks that a Source class doesn't try to declare operations, configs, etc
        SourceDescriptor source = declaration.withMessageSource(getSourceName(sourceType));

        List<Class<?>> sourceGenerics = getSuperClassGenerics(sourceType, Source.class);

        if (sourceGenerics.size() != 2)
        {
            //TODO: MULE-9220: Add a syntax validator for this
            throw new IllegalModelDefinitionException(String.format("Message source class '%s' was expected to have 2 generic types " +
                                                                    "(one for the Payload type and another for the Attributes type) but %d were found",
                                                                    sourceType.getName(), sourceGenerics.size()));
        }

        source.sourceCreatedBy(new DefaultSourceFactory(sourceType))
                .whichReturns(DataType.of(sourceGenerics.get(0)))
                .withAttributesOfType(DataType.of(sourceGenerics.get(1)))
                .withExceptionEnricherFactory(getExceptionEnricherFactory(sourceType))
                .withModelProperty(ImplementingTypeModelProperty.KEY, new ImplementingTypeModelProperty(sourceType));

        declareAnnotatedParameters(sourceType, source, source.with());
    }

    private void declareAnnotatedParameters(Class<?> annotatedType, Descriptor descriptor, WithParameters with)
    {
        declareSingleParameters(getParameterFields(annotatedType), with);
        List<ParameterGroup> groups = declareConfigurationParametersGroups(annotatedType, with, null);
        if (!CollectionUtils.isEmpty(groups) && descriptor instanceof HasModelProperties)
        {
            ((HasModelProperties) descriptor).withModelProperty(ParameterGroupModelProperty.KEY, new ParameterGroupModelProperty(groups));
        }
    }

    private java.util.Optional<ExceptionEnricherFactory> getExceptionEnricherFactory(AnnotatedElement element)
    {
        OnException onExceptionAnnotation = element.getAnnotation(OnException.class);
        if (onExceptionAnnotation != null)
        {
            return java.util.Optional.of(new DefaultExceptionEnricherFactory(onExceptionAnnotation.value()));
        }
        return java.util.Optional.empty();
    }

    private List<ParameterGroup> declareConfigurationParametersGroups(Class<?> annotatedType, WithParameters with, ParameterGroup parent)
    {
        List<ParameterGroup> groups = new LinkedList<>();
        for (Field field : getParameterGroupFields(annotatedType))
        {
            //TODO: MULE-9220
            if (field.isAnnotationPresent(Optional.class))
            {
                throw new IllegalParameterModelDefinitionException(String.format("@%s can not be applied along with @%s. Affected field [%s] in [%s].", Optional.class.getSimpleName(), org.mule.extension.api.annotation.ParameterGroup.class.getSimpleName(), field.getName(), annotatedType));
            }
            Set<ParameterDescriptor> parameters = declareSingleParameters(getExposedFields(field.getType()), with);

            if (!parameters.isEmpty())
            {
                ParameterGroup group = new ParameterGroup(field.getType(), field);
                PlacementModelProperty groupPlacement = null;
                groups.add(group);

                if (field.isAnnotationPresent(Placement.class))
                {
                    Placement placement = field.getAnnotation(Placement.class);
                    groupPlacement = new ImmutablePlacementModelProperty(placement.order(), placement.group(), placement.tab());
                }
                else
                {
                    groupPlacement = parent != null ? parent.getModelProperty(PlacementModelProperty.KEY) : null;
                }

                for (ParameterDescriptor descriptor : parameters)
                {
                    if (groupPlacement != null)
                    {
                        group.addModelProperty(PlacementModelProperty.KEY, groupPlacement);
                        descriptor.withModelProperty(PlacementModelProperty.KEY, groupPlacement);
                    }

                    ParameterDeclaration parameter = descriptor.getDeclaration();
                    group.addParameter(parameter.getName(), getField(field.getType(),
                                                                     getMemberName(parameter, parameter.getName()),
                                                                     parameter.getType().getRawType()));
                }

                List<ParameterGroup> childGroups = declareConfigurationParametersGroups(field.getType(), with, group);
                if (!CollectionUtils.isEmpty(childGroups))
                {
                    group.addModelProperty(ParameterGroupModelProperty.KEY, new ParameterGroupModelProperty(childGroups));
                }
            }
        }

        return groups;
    }

    private Set<ParameterDescriptor> declareSingleParameters(Collection<Field> parameterFields, WithParameters with)
    {
        return parameterFields.stream()
                .map(field -> getFieldDescriber(field).describe(field, with))
                .collect(new ImmutableSetCollector<>());
    }

    private FieldDescriber getFieldDescriber(Field field)
    {
        java.util.Optional<FieldDescriber> describer = fieldDescribers.stream()
                .filter(fieldDescriber -> fieldDescriber.accepts(field))
                .findFirst();

        if (describer.isPresent())
        {
            return describer.get();
        }

        throw new IllegalModelDefinitionException(String.format(
                "Could not find a %s capable of parsing the field '%s' on class '%s'",
                FieldDescriber.class.getSimpleName(), field.getName(), field.getDeclaringClass().getName()));
    }

    private void declareOperations(DeclarationDescriptor declaration, Class<?> extensionType)
    {
        Class<?>[] operations = getOperationClasses(extensionType);
        for (Class<?> actingClass : operations)
        {
            declareOperation(declaration, actingClass);
        }
    }

    private Class<?>[] getOperationClasses(Class<?> extensionType)
    {
        Operations operations = extensionType.getAnnotation(Operations.class);
        return operations == null ? ArrayUtils.EMPTY_CLASS_ARRAY : operations.value();
    }

    private <T> void declareOperation(DeclarationDescriptor declaration, Class<T> actingClass)
    {
        checkOperationIsNotAnExtension(actingClass);

        for (Method method : getOperationMethods(actingClass))
        {
            OperationDescriptor operation = declaration.withOperation(method.getName())
                    .withModelProperty(ImplementingMethodModelProperty.KEY, new ImplementingMethodModelProperty(method))
                    .executorsCreatedBy(new ReflectiveOperationExecutorFactory<>(actingClass, method))
                    .whichReturns(IntrospectionUtils.getMethodReturnType(method))
                    .withExceptionEnricherFactory(getExceptionEnricherFactory(method));

            declareOperationParameters(method, operation);
            calculateExtendedTypes(actingClass, method, operation);
        }
    }

    private void declareConnectionProviders(DeclarationDescriptor declaration, Class<?> extensionType)
    {
        Providers providers = extensionType.getAnnotation(Providers.class);
        if (providers != null)
        {
            for (Class<?> providerClass : providers.value())
            {
                declareConnectionProvider(declaration, providerClass);
            }
        }
    }

    private <T> void declareConnectionProvider(DeclarationDescriptor declaration, Class<T> providerClass)
    {
        String name = DEFAULT_CONNECTION_PROVIDER_NAME;
        String description = EMPTY;

        Alias aliasAnnotation = providerClass.getAnnotation(Alias.class);
        if (aliasAnnotation != null)
        {
            name = aliasAnnotation.value() + CUSTOM_CONNECTION_PROVIDER_SUFFIX;
            description = aliasAnnotation.description();
        }

        List<Class<?>> providerGenerics = getInterfaceGenerics(providerClass, ConnectionProvider.class);

        if (providerGenerics.size() != 2)
        {
            //TODO: MULE-9220: Add a syntax validator for this
            throw new IllegalConnectionProviderModelDefinitionException(String.format("Connection provider class '%s' was expected to have 2 generic types " +
                                                                                      "(one for the config type and another for the connection type) but %d were found",
                                                                                      providerClass.getName(), providerGenerics.size()));
        }

        ConnectionProviderDescriptor providerDescriptor = declaration.withConnectionProvider(name)
                .describedAs(description)
                .createdWith(new DefaultConnectionProviderFactory<>(providerClass))
                .forConfigsOfType(providerGenerics.get(0))
                .whichGivesConnectionsOfType(providerGenerics.get(1))
                .withModelProperty(ImplementingTypeModelProperty.KEY, new ImplementingTypeModelProperty(providerClass));

        declareAnnotatedParameters(providerClass, providerDescriptor, providerDescriptor.with());
    }

    private void calculateExtendedTypes(Class<?> actingClass, Method method, OperationDescriptor operation)
    {
        ExtensionOf extensionOf = method.getAnnotation(ExtensionOf.class);
        if (extensionOf == null)
        {
            extensionOf = actingClass.getAnnotation(ExtensionOf.class);
        }

        if (extensionOf != null)
        {
            operation.withModelProperty(ExtendingOperationModelProperty.KEY, new ExtendingOperationModelProperty(extensionOf.value()));
        }
        else if (isExtensible())
        {
            operation.withModelProperty(ExtendingOperationModelProperty.KEY, new ExtendingOperationModelProperty(extensionType));
        }
    }

    private boolean isExtensible()
    {
        return extensionType.getAnnotation(Extensible.class) != null;
    }

    private void declareOperationParameters(Method method, OperationDescriptor operation)
    {
        List<ParsedParameter> descriptors = MuleExtensionAnnotationParser.parseParameters(method);

        //TODO: MULE-9220
        checkAnnotationIsNotUsedMoreThanOnce(method, operation, UseConfig.class);
        checkAnnotationIsNotUsedMoreThanOnce(method, operation, Connection.class);

        for (ParsedParameter parsedParameter : descriptors)
        {
            if (parsedParameter.isAdvertised())
            {
                ParameterDescriptor parameter = parsedParameter.isRequired()
                                                ? operation.with().requiredParameter(parsedParameter.getName())
                                                : operation.with().optionalParameter(parsedParameter.getName()).defaultingTo(parsedParameter.getDefaultValue());

                parameter.withExpressionSupport(IntrospectionUtils.getExpressionSupport(parsedParameter));
                parameter.describedAs(EMPTY).ofType(parsedParameter.getType());
                addTypeRestrictions(parameter, parsedParameter);
                parseDisplayAnnotations(parsedParameter, parameter);
            }

            Connection connectionAnnotation = parsedParameter.getAnnotation(Connection.class);
            if (connectionAnnotation != null)
            {
                operation.withModelProperty(ConnectionTypeModelProperty.KEY, new ConnectionTypeModelProperty(parsedParameter.getType().getRawType()));
            }

            UseConfig useConfig = parsedParameter.getAnnotation(UseConfig.class);
            if (useConfig != null)
            {
                operation.withModelProperty(ConfigTypeModelProperty.KEY, new ConfigTypeModelProperty(parsedParameter.getType().getRawType()));
            }
        }
    }

    private void checkAnnotationIsNotUsedMoreThanOnce(Method method, OperationDescriptor operation, Class annotationClass)
    {
        Stream<java.lang.reflect.Parameter> parametersStream = Arrays
                .stream(method.getParameters())
                .filter(parameter -> parameter.isAnnotationPresent(annotationClass));

        List<java.lang.reflect.Parameter> parameterList = parametersStream.collect(Collectors.toList());

        if (parameterList.size() > 1)
        {
            throw new IllegalModelDefinitionException(String.format("Method [%s] defined in Class [%s] of extension [%s] uses the annotation @%s more than once", method.getName(), method.getDeclaringClass(), operation.getRootDeclaration().getDeclaration().getName(), annotationClass.getSimpleName()));
        }
    }

    private void addTypeRestrictions(ParameterDescriptor parameter, ParsedParameter descriptor)
    {
        Class<?> restriction = descriptor.getTypeRestriction();
        if (restriction != null)
        {
            parameter.withModelProperty(TypeRestrictionModelProperty.KEY, new TypeRestrictionModelProperty<>(restriction));
        }
    }
}