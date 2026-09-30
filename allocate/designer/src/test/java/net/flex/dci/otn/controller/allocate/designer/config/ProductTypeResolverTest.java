/*
 * Copyright (c) 2019 Network Flex Any Comp. and others and others. All rights reserved.
 */
package net.flex.dci.otn.controller.allocate.designer.config;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class ProductTypeResolverTest {

    @Test
    void bone20ProductTypeIsNormalized() {
        assertEquals("CHASSIS2.0", ProductTypeResolver.resolveProductType("COHERENT", "CHASSIS2.0"));
    }

    @Test
    void bone20ProductTypeIsCaseInsensitive() {
        assertEquals("CHASSIS2.0", ProductTypeResolver.resolveProductType("coherent", "chassis2.0"));
    }

    @Test
    void legacyProductIsNotMapped() {
        assertEquals("CHASSIS", ProductTypeResolver.resolveProductType("COHERENT", "CHASSIS"));
    }

    @Test
    void unrelatedVendorIsNotMapped() {
        assertEquals("CHASSIS2.0", ProductTypeResolver.resolveProductType("OTHER", "CHASSIS2.0"));
    }
}
