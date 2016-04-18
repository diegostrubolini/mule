/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.vegan;

import org.mule.extension.api.annotation.Configurations;
import org.mule.extension.api.annotation.Extension;

@Extension(name = VeganExtension.VEGAN)
@Configurations({AppleConfig.class, BananaConfig.class, KiwiConfig.class})
public class VeganExtension
{
    public static final String VEGAN = "vegan";
    public static final String APPLE = "apple-config";
    public static final String BANANA = "banana-config";
    public static final String KIWI = "kiwi-config";

}