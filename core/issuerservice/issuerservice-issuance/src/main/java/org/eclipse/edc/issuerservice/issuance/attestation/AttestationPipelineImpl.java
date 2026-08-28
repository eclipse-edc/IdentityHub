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
import org.eclipse.edc.spi.result.Result;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;


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
            var definition = store.resolveDefinition(attestationId);
            if (definition == null) {
                return Result.failure("Attestation with ID '%s' not found".formatted(attestationId));
            }

            var factory = factories.get(definition.getAttestationType());
            if (factory == null) {
                return Result.failure("Attestation Type  '%s' not found".formatted(definition.getAttestationType()));
            }

            var source = factory.createSource(definition);
            if (source == null) {
                return Result.failure("Invalid definition for type: " + definition.getAttestationType());
            }
            var result = source.execute(context);
            if (result.failed()) {
                return result;
            }
            collated.putAll(result.getContent());
        }
        return Result.success(collated);
    }

}


