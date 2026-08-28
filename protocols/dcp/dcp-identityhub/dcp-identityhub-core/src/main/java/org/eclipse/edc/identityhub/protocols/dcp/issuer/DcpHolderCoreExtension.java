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

import com.nimbusds.jwt.SignedJWT;
import org.eclipse.edc.iam.did.spi.resolution.DidPublicKeyResolver;
import org.eclipse.edc.identityhub.protocols.dcp.spi.DcpIssuerTokenVerifier;
import org.eclipse.edc.jwt.validation.jti.JtiValidationStore;
import org.eclipse.edc.runtime.metamodel.annotation.Extension;
import org.eclipse.edc.runtime.metamodel.annotation.Inject;
import org.eclipse.edc.runtime.metamodel.annotation.Provider;
import org.eclipse.edc.runtime.metamodel.annotation.Setting;
import org.eclipse.edc.spi.EdcException;
import org.eclipse.edc.spi.result.Result;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.eclipse.edc.token.rules.AudienceValidationRule;
import org.eclipse.edc.token.rules.ExpirationIssuedAtValidationRule;
import org.eclipse.edc.token.rules.JtiValidationRule;
import org.eclipse.edc.token.rules.NotBeforeValidationRule;
import org.eclipse.edc.token.spi.TokenValidationRule;
import org.eclipse.edc.token.spi.TokenValidationService;
import org.eclipse.edc.verifiablecredentials.jwt.rules.IssuerEqualsSubjectRule;
import org.eclipse.edc.verifiablecredentials.jwt.rules.IssuerKeyIdValidationRule;

import java.text.ParseException;
import java.time.Clock;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Extension("DCP Holder Core Extension")
public class DcpHolderCoreExtension implements ServiceExtension {

    static final String ACCESSTOKEN_JTI_VALIDATION_ACTIVATE = "edc.iam.accesstoken.jti.validation";
    private List<TokenValidationRule> rules;
    @Inject
    private Clock clock;
    @Inject
    private TokenValidationService tokenValidationService;
    @Inject
    private DidPublicKeyResolver didPublicKeyResolver;
    @Setting(description = "Activates the JTI check: access tokens can only be used once to guard against replay attacks", defaultValue = "false", key = ACCESSTOKEN_JTI_VALIDATION_ACTIVATE)
    private boolean activateJtiCheck;

    @Inject(required = false)
    private JtiValidationStore jtiValidationStore;

    @Override
    public void initialize(ServiceExtensionContext context) {
        rules = new ArrayList<>();
        if (activateJtiCheck) {
            if (jtiValidationStore == null) {
                throw new EdcException("JTI validation is activated ('%s') but no JtiValidationStore is provided.".formatted(ACCESSTOKEN_JTI_VALIDATION_ACTIVATE));
            }
            rules.add(new JtiValidationRule(jtiValidationStore, context.getMonitor()));
        }
        rules.addAll(List.of(
                new IssuerEqualsSubjectRule(),
                new NotBeforeValidationRule(clock, 5, true),
                new ExpirationIssuedAtValidationRule(clock, 5, false)
                //todo: add rule to only allow trusted issuers
        ));
    }

    @Provider
    public DcpIssuerTokenVerifier createTokenVerifier() {
        return (participantContext, tokenRepresentation) -> {
            // the signing key is resolved from the DID named in the 'kid' header, while the sender identifies itself
            // with the 'iss' claim. Unless the two are tied together, anyone holding a resolvable DID could sign a
            // message that claims to come from somebody else.
            var kid = getKid(tokenRepresentation.getToken());
            if (kid.failed()) {
                return kid.mapFailure();
            }

            var newRules = new ArrayList<>(rules);
            newRules.add(new AudienceValidationRule(participantContext.getDid()));
            newRules.add(new IssuerKeyIdValidationRule(kid.getContent()));
            return tokenValidationService.validate(tokenRepresentation.getToken(), didPublicKeyResolver, newRules);
        };
    }

    private Result<String> getKid(String token) {
        try {
            return Optional.ofNullable(SignedJWT.parse(token).getHeader().getKeyID())
                    .map(Result::success)
                    .orElseGet(() -> Result.failure("Kid not present"));
        } catch (ParseException e) {
            return Result.failure("Failed to decode token");
        }
    }
}
