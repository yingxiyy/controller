/*
 * Copyright (c) 2019 Network Flex Any Comp. and others and others. All rights reserved.
 */
package net.flex.dci.otn.controller.allocate.designer.config;

/**
 * Normalizes product-type based Bone2.0 detection.
 * CHASSIS2.0 is the single identity used by allocation code.
 */
public final class ProductTypeResolver {

    public static final String COHERENT_VENDOR_NAME = "COHERENT";
    public static final String BONE20_PRODUCT_TYPE = "CHASSIS2.0";

    private ProductTypeResolver() {
    }

    public static String resolveProductType(String vendorName, String productType) {
        return isBone20ProductType(vendorName, productType) ? BONE20_PRODUCT_TYPE : productType;
    }

    public static boolean isBone20ProductType(String vendorName, String productType) {
        return COHERENT_VENDOR_NAME.equalsIgnoreCase(vendorName)
                && BONE20_PRODUCT_TYPE.equalsIgnoreCase(productType);
    }

}
