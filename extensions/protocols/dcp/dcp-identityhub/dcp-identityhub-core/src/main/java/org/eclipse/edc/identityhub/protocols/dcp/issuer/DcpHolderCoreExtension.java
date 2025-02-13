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

package org.eclipse.edc.identityhub.protocols.dcp.issuer;

import org.eclipse.edc.iam.did.spi.resolution.DidPublicKeyResolver;
import org.eclipse.edc.identityhub.protocols.dcp.identityhub.spi.DcpIssuerTokenVerifier;
import org.eclipse.edc.runtime.metamodel.annotation.Extension;
import org.eclipse.edc.runtime.metamodel.annotation.Inject;
import org.eclipse.edc.runtime.metamodel.annotation.Provider;
import org.eclipse.edc.runtime.metamodel.annotation.Setting;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.eclipse.edc.token.rules.AudienceValidationRule;
import org.eclipse.edc.token.rules.ExpirationIssuedAtValidationRule;
import org.eclipse.edc.token.rules.NotBeforeValidationRule;
import org.eclipse.edc.token.spi.TokenValidationRule;
import org.eclipse.edc.token.spi.TokenValidationService;
import org.eclipse.edc.verifiablecredentials.jwt.rules.IssuerEqualsSubjectRule;

import java.time.Clock;
import java.util.List;

@Extension("DCP Holder Core Extension")
public class DcpHolderCoreExtension implements ServiceExtension {

    private List<TokenValidationRule> rules;
    @Inject
    private Clock clock;
    @Inject
    private TokenValidationService tokenValidationService;
    @Setting(key = "edc.ih.iam.id", description = "DID of the holder")
    private String ownDid;
    @Inject
    private DidPublicKeyResolver didPublicKeyResolver;

    @Override
    public void initialize(ServiceExtensionContext context) {
        rules = List.of(
                new IssuerEqualsSubjectRule(),
                new NotBeforeValidationRule(clock, 5, true),
                new AudienceValidationRule(ownDid),
                new ExpirationIssuedAtValidationRule(clock, 5, false)
                //todo: add rule to only allow trusted issuers
        );
    }

    @Provider
    public DcpIssuerTokenVerifier createTokenVerifier() {
        return tokenRepresentation -> tokenValidationService.validate(tokenRepresentation.getToken(), didPublicKeyResolver, rules);
    }
}
