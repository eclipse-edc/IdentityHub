/*
 *  Copyright (c) 2024 Metaform Systems, Inc.
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Metaform Systems Inc. - initial API and implementation
 *
 */

package org.eclipse.edc.identityhub.defaults.store;


import com.apicatalog.vc.suite.SignatureSuite;
import org.eclipse.edc.iam.identitytrust.spi.verification.SignatureSuiteRegistry;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class InMemorySignatureSuiteRegistry implements SignatureSuiteRegistry {
    private final Map<String, SignatureSuite> registry = new HashMap<>();

    @Override
    public void register(String w3cIdentifier, SignatureSuite suite) {
        registry.put(w3cIdentifier, suite);
    }

    @Override
    public SignatureSuite getForId(String w3cIdentifier) {
        return registry.get(w3cIdentifier);
    }

    @Override
    public Collection<SignatureSuite> getAllSuites() {
        return registry.values();
    }
}
