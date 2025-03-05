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

package org.eclipse.edc.issuerservice.issuance;

import org.eclipse.edc.iam.verifiablecredentials.spi.model.CredentialFormat;
import org.eclipse.edc.identityhub.spi.keypair.KeyPairService;
import org.eclipse.edc.identityhub.spi.participantcontext.ParticipantContextService;
import org.eclipse.edc.issuerservice.issuance.attestation.AttestationDefinitionServiceImpl;
import org.eclipse.edc.issuerservice.issuance.attestation.AttestationDefinitionValidatorRegistryImpl;
import org.eclipse.edc.issuerservice.issuance.attestation.AttestationPipelineImpl;
import org.eclipse.edc.issuerservice.issuance.credentialdefinition.CredentialDefinitionServiceImpl;
import org.eclipse.edc.issuerservice.issuance.generator.CredentialGeneratorRegistryImpl;
import org.eclipse.edc.issuerservice.issuance.generator.JwtCredentialGenerator;
import org.eclipse.edc.issuerservice.issuance.mapping.IssuanceClaimsMapperImpl;
import org.eclipse.edc.issuerservice.issuance.rule.CredentialRuleDefinitionEvaluatorImpl;
import org.eclipse.edc.issuerservice.issuance.rule.CredentialRuleDefinitionValidatorRegistryImpl;
import org.eclipse.edc.issuerservice.issuance.rule.CredentialRuleFactoryRegistryImpl;
import org.eclipse.edc.issuerservice.spi.holder.HolderService;
import org.eclipse.edc.issuerservice.spi.issuance.attestation.AttestationDefinitionService;
import org.eclipse.edc.issuerservice.spi.issuance.attestation.AttestationDefinitionStore;
import org.eclipse.edc.issuerservice.spi.issuance.attestation.AttestationDefinitionValidatorRegistry;
import org.eclipse.edc.issuerservice.spi.issuance.attestation.AttestationPipeline;
import org.eclipse.edc.issuerservice.spi.issuance.attestation.AttestationSourceFactoryRegistry;
import org.eclipse.edc.issuerservice.spi.issuance.credentialdefinition.CredentialDefinitionService;
import org.eclipse.edc.issuerservice.spi.issuance.credentialdefinition.store.CredentialDefinitionStore;
import org.eclipse.edc.issuerservice.spi.issuance.generator.CredentialGeneratorRegistry;
import org.eclipse.edc.issuerservice.spi.issuance.mapping.IssuanceClaimsMapper;
import org.eclipse.edc.issuerservice.spi.issuance.rule.CredentialRuleDefinitionEvaluator;
import org.eclipse.edc.issuerservice.spi.issuance.rule.CredentialRuleDefinitionValidatorRegistry;
import org.eclipse.edc.issuerservice.spi.issuance.rule.CredentialRuleFactoryRegistry;
import org.eclipse.edc.jwt.signer.spi.JwsSignerProvider;
import org.eclipse.edc.runtime.metamodel.annotation.Extension;
import org.eclipse.edc.runtime.metamodel.annotation.Inject;
import org.eclipse.edc.runtime.metamodel.annotation.Provider;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.token.JwtGenerationService;
import org.eclipse.edc.transaction.spi.TransactionContext;

import java.time.Clock;

import static org.eclipse.edc.issuerservice.issuance.IssuanceServicesExtension.NAME;

@Extension(value = NAME)
public class IssuanceServicesExtension implements ServiceExtension {

    public static final String NAME = "IssuerService Issuance Services Extension";

    @Inject
    private TransactionContext transactionContext;
    @Inject
    private CredentialDefinitionStore store;
    @Inject
    private AttestationDefinitionStore attestationDefinitionStore;

    @Inject
    private KeyPairService keyPairService;

    @Inject
    private JwsSignerProvider jwsSignerProvider;

    @Inject
    private HolderService holderService;

    @Inject
    private Clock clock;

    @Inject
    private ParticipantContextService participantContextService;

    private AttestationPipelineImpl attestationPipeline;

    private CredentialRuleFactoryRegistry ruleFactoryRegistry;

    private CredentialRuleDefinitionValidatorRegistry ruleDefinitionValidatorRegistry;

    private AttestationDefinitionValidatorRegistry attestationDefinitionValidatorRegistry;

    private IssuanceClaimsMapper issuanceClaimsMapper;

    @Provider
    public CredentialDefinitionService createParticipantService() {
        return new CredentialDefinitionServiceImpl(transactionContext, store, attestationDefinitionStore, credentialRuleDefinitionValidatorRegistry());
    }

    @Provider
    public AttestationDefinitionService createAttestationService() {
        return new AttestationDefinitionServiceImpl(transactionContext, attestationDefinitionStore, createAttestationDefinitionValidatorRegistry());
    }

    @Provider
    public AttestationPipeline createAttestationPipeline() {
        return createAttestationPipelineImpl();
    }

    @Provider
    public AttestationSourceFactoryRegistry createAttestationSourceFactoryRegistry() {
        return createAttestationPipelineImpl();
    }

    @Provider
    public CredentialRuleDefinitionEvaluator credentialRuleDefinitionEvaluator() {
        return new CredentialRuleDefinitionEvaluatorImpl(credentialRuleFactoryRegistry());
    }


    @Provider
    public IssuanceClaimsMapper issuanceClaimsMapper() {
        if (issuanceClaimsMapper == null) {
            issuanceClaimsMapper = new IssuanceClaimsMapperImpl();
        }
        return issuanceClaimsMapper;
    }

    @Provider
    public CredentialRuleFactoryRegistry credentialRuleFactoryRegistry() {

        if (ruleFactoryRegistry == null) {
            ruleFactoryRegistry = new CredentialRuleFactoryRegistryImpl();
        }
        return ruleFactoryRegistry;
    }

    @Provider
    public CredentialRuleDefinitionValidatorRegistry credentialRuleDefinitionValidatorRegistry() {
        if (ruleDefinitionValidatorRegistry == null) {
            ruleDefinitionValidatorRegistry = new CredentialRuleDefinitionValidatorRegistryImpl();
        }
        return ruleDefinitionValidatorRegistry;
    }

    @Provider
    public CredentialGeneratorRegistry createCredentialGeneratorRegistry() {
        var generator = new CredentialGeneratorRegistryImpl(issuanceClaimsMapper(), participantContextService, holderService, keyPairService);

        var jwtGenerationService = new JwtGenerationService(jwsSignerProvider);
        generator.addGenerator(CredentialFormat.VC1_0_JWT, new JwtCredentialGenerator(jwtGenerationService, clock));
        return generator;
    }

    @Provider
    public AttestationDefinitionValidatorRegistry createAttestationDefinitionValidatorRegistry() {
        if (attestationDefinitionValidatorRegistry == null) {
            attestationDefinitionValidatorRegistry = new AttestationDefinitionValidatorRegistryImpl();
        }
        return attestationDefinitionValidatorRegistry;
    }

    private AttestationPipelineImpl createAttestationPipelineImpl() {
        if (attestationPipeline == null) {
            attestationPipeline = new AttestationPipelineImpl(attestationDefinitionStore);
        }
        return attestationPipeline;
    }
}
