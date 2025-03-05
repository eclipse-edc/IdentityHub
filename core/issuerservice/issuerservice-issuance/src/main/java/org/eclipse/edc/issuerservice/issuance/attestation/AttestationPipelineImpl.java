/*
 *  Copyright (c) 2025 Cofinity-X
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Cofinity-X - initial API and implementation
 *
 */

package org.eclipse.edc.issuerservice.issuance.attestation;

import org.eclipse.edc.issuerservice.spi.issuance.attestation.AttestationContext;
import org.eclipse.edc.issuerservice.spi.issuance.attestation.AttestationDefinitionStore;
import org.eclipse.edc.issuerservice.spi.issuance.attestation.AttestationPipeline;
import org.eclipse.edc.issuerservice.spi.issuance.attestation.AttestationSourceFactory;
import org.eclipse.edc.issuerservice.spi.issuance.attestation.AttestationSourceFactoryRegistry;
import org.eclipse.edc.issuerservice.spi.issuance.model.AttestationDefinition;
import org.eclipse.edc.spi.result.Result;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import static java.util.Objects.requireNonNull;

/**
 * Holds registered {@link AttestationSourceFactory}s that performs attestation pipeline evaluations.
 */
public class AttestationPipelineImpl implements AttestationPipeline, AttestationSourceFactoryRegistry {
    private final Map<String, AttestationSourceFactory> factories = new HashMap<>();
    private final AttestationDefinitionStore store;

    public AttestationPipelineImpl(AttestationDefinitionStore store) {
        this.store = store;
    }

    @Override
    public Set<String> registeredTypes() {
        return factories.keySet();
    }

    @Override
    public void registerFactory(String type, AttestationSourceFactory factory) {
        factories.put(type, factory);
    }

    @Override
    public Result<Map<String, Object>> evaluate(Set<String> attestations, AttestationContext context) {
        var collated = new HashMap<String, Object>();
        for (var attestationId : attestations) {
            // if the attestation is not found it is a programming error
            var definition = requireNonNull(store.resolveDefinition(attestationId), "Unknown attestation: " + attestationId);
            var result = execute(definition, context);
            if (result.failed()) {
                return result;
            }
            collated.putAll(result.getContent());
        }
        return Result.success(collated);
    }

    private Result<Map<String, Object>> execute(AttestationDefinition definition, AttestationContext context) {
        var factory = requireNonNull(factories.get(definition.getAttestationType()), "Unknown attestation type: " + definition.getAttestationType());
        return requireNonNull(factory.createSource(definition), "Invalid definition for type: " + definition.getAttestationType()).execute(context);
    }
}


