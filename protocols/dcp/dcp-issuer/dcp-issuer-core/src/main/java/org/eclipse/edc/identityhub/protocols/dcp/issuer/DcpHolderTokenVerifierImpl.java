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
import org.eclipse.edc.iam.identitytrust.spi.validation.TokenValidationAction;
<<<<<<<< HEAD:protocols/dcp/dcp-issuer/dcp-issuer-core/src/main/java/org/eclipse/edc/identityhub/protocols/dcp/issuer/DcpIssuerSelfIssuedTokenVerifierImpl.java
import org.eclipse.edc.identityhub.protocols.dcp.issuer.spi.DcpIssuerSelfIssuedTokenVerifier;
import org.eclipse.edc.identityhub.protocols.dcp.spi.model.DcpRequestContext;
========
import org.eclipse.edc.identityhub.protocols.dcp.issuer.spi.DcpHolderTokenVerifier;
import org.eclipse.edc.identityhub.protocols.dcp.issuer.spi.model.DcpRequestContext;
>>>>>>>> 3864ba87 (feat: add credential writer and issuer token verifier):protocols/dcp/dcp-issuer/dcp-issuer-core/src/main/java/org/eclipse/edc/identityhub/protocols/dcp/issuer/DcpHolderTokenVerifierImpl.java
import org.eclipse.edc.issuerservice.spi.participant.model.Participant;
import org.eclipse.edc.issuerservice.spi.participant.store.ParticipantStore;
import org.eclipse.edc.jwt.spi.JwtRegisteredClaimNames;
import org.eclipse.edc.spi.iam.TokenRepresentation;
import org.eclipse.edc.spi.query.Criterion;
import org.eclipse.edc.spi.query.QuerySpec;
import org.eclipse.edc.spi.result.ServiceResult;

import java.text.ParseException;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;

public class DcpHolderTokenVerifierImpl implements DcpHolderTokenVerifier {

    private final ParticipantStore store;
    private final TokenValidationAction tokenValidation;

    public DcpHolderTokenVerifierImpl(ParticipantStore store, TokenValidationAction tokenValidation) {
        this.store = store;
        this.tokenValidation = tokenValidation;
    }


    @Override
    public ServiceResult<DcpRequestContext> verify(TokenRepresentation tokenRepresentation) {
        return getTokenIssuer(tokenRepresentation.getToken())
                .compose(this::getParticipant)
                .compose(participant -> validateToken(tokenRepresentation, participant));
    }

    private ServiceResult<String> getTokenIssuer(String token) {
        try {
            return Optional.ofNullable(SignedJWT.parse(token).getJWTClaimsSet().getClaim(JwtRegisteredClaimNames.ISSUER))
                    .map(Object::toString)
                    .map(ServiceResult::success)
                    .orElseGet(() -> ServiceResult.unauthorized("Issuer claim not present"));
        } catch (ParseException e) {
            return ServiceResult.badRequest("Failed to decode token");
        }
    }

    private ServiceResult<Participant> getParticipant(String issuer) {
        var query = QuerySpec.Builder.newInstance().filter(Criterion.criterion("did", "=", issuer)).build();
        return ServiceResult.from(store.query(query)).compose(this::findFirst);
    }

    private ServiceResult<Participant> findFirst(Collection<Participant> participants) {
        return participants.stream().findFirst()
                .map(ServiceResult::success)
                .orElseGet(() -> ServiceResult.unauthorized("Participant not found"));
    }

    private ServiceResult<DcpRequestContext> validateToken(TokenRepresentation token, Participant participant) {
        var res = tokenValidation.apply(token);
        if (res.failed()) {
            return ServiceResult.unauthorized("Token validation failed");
        }
        return ServiceResult.success(new DcpRequestContext(participant, Map.of()));
    }
}
