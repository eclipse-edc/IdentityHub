/*
 *  Copyright (c) 2026 Metaform Systems, Inc.
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Metaform Systems, Inc. - initial API and implementation
 *
 */

package org.eclipse.edc.identityhub.tests.fixtures;

import com.nimbusds.jose.jwk.ECKey;
import com.nimbusds.jose.util.JSONObjectUtils;
import com.nimbusds.jwt.JWTClaimsSet;
import org.eclipse.edc.iam.verifiablecredentials.spi.model.CredentialFormat;
import org.eclipse.edc.iam.verifiablecredentials.spi.model.CredentialSubject;
import org.eclipse.edc.iam.verifiablecredentials.spi.model.VerifiableCredential;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.eclipse.edc.iam.verifiablecredentials.spi.VcConstants.VC_PREFIX_V2;
import static org.eclipse.edc.iam.verifiablecredentials.spi.VcConstants.W3C_CREDENTIALS_URL;
import static org.eclipse.edc.identityhub.verifiablecredentials.testfixtures.VerifiableCredentialTestUtil.buildSignedJwt;
import static org.eclipse.edc.identityhub.verifiablecredentials.testfixtures.VerifiableCredentialTestUtil.generateEcKey;

/**
 * Creates the raw form of a credential in a given {@link CredentialFormat}, so that test credentials are stored in the format
 * they are declared as. The credential is given as a VCDM 1.1 JSON-LD document and is
 * <ul>
 *     <li>returned as it is for {@link CredentialFormat#VC1_0_LD}</li>
 *     <li>put into the {@code vc} claim of a JWT for {@link CredentialFormat#VC1_0_JWT}</li>
 *     <li>converted to VCDM 2.0 and secured as a JWT for {@link CredentialFormat#VC2_0_JOSE}</li>
 * </ul>
 * The JWTs are signed with a key generated for each test run, so their signatures cannot be verified with any issuer's key.
 */
public final class RawCredentials {
    private static final String CONTEXT = "@context";
    private static final String PROOF = "proof";
    private static final ECKey SIGNING_KEY = generateEcKey("did:web:test-issuer#key1");

    private RawCredentials() {
    }

    public static String rawCredential(String jsonLdCredential, CredentialFormat format) {
        try {
            return rawCredential(JSONObjectUtils.parse(jsonLdCredential), format, jsonLdCredential);
        } catch (ParseException e) {
            throw new IllegalArgumentException("Not a JSON-LD credential", e);
        }
    }

    public static String rawCredential(VerifiableCredential credential, CredentialFormat format) {
        var jsonLd = new LinkedHashMap<String, Object>();
        jsonLd.put(CONTEXT, List.of(W3C_CREDENTIALS_URL));
        if (credential.getId() != null) {
            jsonLd.put("id", credential.getId());
        }
        jsonLd.put("type", credential.getType());
        jsonLd.put("issuer", credential.getIssuer().id());
        if (credential.getIssuanceDate() != null) {
            jsonLd.put("issuanceDate", credential.getIssuanceDate().toString());
        }
        jsonLd.put("credentialSubject", credential.getCredentialSubject().stream().map(RawCredentials::subject).toList());
        return rawCredential(jsonLd, format, JSONObjectUtils.toJSONString(jsonLd));
    }

    private static String rawCredential(Map<String, Object> jsonLd, CredentialFormat format, String jsonLdString) {
        return switch (format) {
            case VC1_0_LD -> jsonLdString;
            case VC1_0_JWT -> vcdm11Jwt(jsonLd);
            case VC2_0_JOSE -> vcdm20Jwt(jsonLd);
            default -> throw new IllegalArgumentException("Credentials cannot be created in format " + format);
        };
    }

    private static String vcdm11Jwt(Map<String, Object> jsonLd) {
        var vc = new HashMap<>(jsonLd);
        vc.remove(PROOF);
        var claims = new JWTClaimsSet.Builder()
                .issuer(issuerId(vc.get("issuer")))
                .jwtID((String) vc.get("id"))
                .claim("vc", vc);
        subjectId(vc.get("credentialSubject")).ifPresent(claims::subject);
        return buildSignedJwt(claims.build(), SIGNING_KEY).serialize();
    }

    private static String vcdm20Jwt(Map<String, Object> jsonLd) {
        var claims = new JWTClaimsSet.Builder();
        jsonLd.forEach((key, value) -> {
            switch (key) {
                case CONTEXT -> claims.claim(CONTEXT, vcdm20Contexts(value));
                case "issuanceDate" -> claims.claim("validFrom", value);
                case "expirationDate" -> claims.claim("validUntil", value);
                case PROOF -> {
                }
                default -> claims.claim(key, value);
            }
        });
        return buildSignedJwt(claims.build(), SIGNING_KEY).serialize();
    }

    private static List<Object> vcdm20Contexts(Object contexts) {
        var result = new ArrayList<>();
        result.add(VC_PREFIX_V2);
        var existing = contexts instanceof Collection<?> list ? list : List.of(contexts);
        existing.stream().filter(c -> !W3C_CREDENTIALS_URL.equals(c) && !VC_PREFIX_V2.equals(c)).forEach(result::add);
        return result;
    }

    private static String issuerId(Object issuer) {
        return issuer instanceof Map<?, ?> issuerObject ? (String) issuerObject.get("id") : (String) issuer;
    }

    private static Optional<String> subjectId(Object subject) {
        var first = subject instanceof Collection<?> subjects ? subjects.stream().findFirst().orElse(null) : subject;
        return first instanceof Map<?, ?> subjectObject ? Optional.ofNullable((String) subjectObject.get("id")) : Optional.empty();
    }

    private static Map<String, Object> subject(CredentialSubject subject) {
        var result = new LinkedHashMap<String, Object>(subject.getClaims());
        if (subject.getId() != null) {
            result.put("id", subject.getId());
        }
        return result;
    }
}
