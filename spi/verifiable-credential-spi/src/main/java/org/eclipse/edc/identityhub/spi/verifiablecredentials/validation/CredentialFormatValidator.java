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

package org.eclipse.edc.identityhub.spi.verifiablecredentials.validation;

import com.nimbusds.jose.util.JSONObjectUtils;
import com.nimbusds.jwt.SignedJWT;
import org.eclipse.edc.iam.verifiablecredentials.spi.model.CredentialFormat;
import org.eclipse.edc.iam.verifiablecredentials.spi.model.VerifiableCredentialContainer;
import org.eclipse.edc.spi.result.Result;
import org.eclipse.edc.validator.spi.ValidationResult;
import org.eclipse.edc.validator.spi.Validator;

import java.text.ParseException;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import static org.eclipse.edc.iam.verifiablecredentials.spi.VcConstants.VC_PREFIX_V2;
import static org.eclipse.edc.iam.verifiablecredentials.spi.VcConstants.W3C_CREDENTIALS_URL;
import static org.eclipse.edc.validator.spi.Violation.violation;

/**
 * Validates that the raw credential of a {@link VerifiableCredentialContainer} is what its declared {@link CredentialFormat}
 * says it is, i.e. that it is of the declared data model (VCDM 1.1 or 2.0) and in the declared representation. A credential
 * stored under the wrong format would later be put into a presentation of the wrong data model, which verifiers reject.
 * Formats that cannot be presented, i.e. {@link CredentialFormat#VC2_0_SD_JWT} and {@link CredentialFormat#VC2_0_COSE}, are rejected.
 * <ul>
 *     <li>{@link CredentialFormat#VC1_0_JWT}: a JWT with a {@code vc} claim</li>
 *     <li>{@link CredentialFormat#VC2_0_JOSE}: a JWT without a {@code vc} claim, whose {@code @context} contains the VCDM 2.0 context</li>
 *     <li>{@link CredentialFormat#VC1_0_LD}: a JSON object whose {@code @context} contains the VCDM 1.1 context, but not the VCDM 2.0 context</li>
 * </ul>
 * Only the raw credential is inspected, never the parsed credential of the container, as that may have been supplied separately.
 */
public class CredentialFormatValidator implements Validator<VerifiableCredentialContainer> {
    private static final String VC_CLAIM = "vc";
    private static final String CONTEXT_PROPERTY = "@context";
    private static final String RAW_VC_PATH = "rawVc";

    @Override
    public ValidationResult validate(VerifiableCredentialContainer container) {
        var format = container.format();
        if (format == null) {
            return ValidationResult.failure(violation("The credential format is missing", "format"));
        }
        var rawCredential = container.rawVc();
        if (rawCredential == null || rawCredential.isBlank()) {
            return ValidationResult.failure(violation("The raw credential is missing", RAW_VC_PATH));
        }
        var result = switch (format) {
            case VC1_0_JWT -> jwtClaims(rawCredential, format)
                    .compose(claims -> claims.containsKey(VC_CLAIM)
                            ? Result.<Void>success()
                            : Result.<Void>failure("A '%s' credential must be a JWT with a '%s' claim".formatted(format, VC_CLAIM)));
            case VC2_0_JOSE -> jwtClaims(rawCredential, format)
                    .compose(claims -> !claims.containsKey(VC_CLAIM) && contexts(claims).contains(VC_PREFIX_V2)
                            ? Result.<Void>success()
                            : Result.<Void>failure("A '%s' credential must be a JWT without a '%s' claim whose '%s' contains '%s'".formatted(format, VC_CLAIM, CONTEXT_PROPERTY, VC_PREFIX_V2)));
            case VC1_0_LD -> jsonObject(rawCredential, format)
                    .compose(json -> {
                        var contexts = contexts(json);
                        return contexts.contains(W3C_CREDENTIALS_URL) && !contexts.contains(VC_PREFIX_V2)
                                ? Result.<Void>success()
                                : Result.<Void>failure("A '%s' credential must have a '%s' that contains '%s' and not '%s'".formatted(format, CONTEXT_PROPERTY, W3C_CREDENTIALS_URL, VC_PREFIX_V2));
                    });
            case VC2_0_SD_JWT, VC2_0_COSE -> Result.<Void>failure("Credentials in format '%s' cannot be presented".formatted(format));
        };
        return result.succeeded()
                ? ValidationResult.success()
                : ValidationResult.failure(violation(result.getFailureDetail(), RAW_VC_PATH));
    }

    private Result<Map<String, Object>> jwtClaims(String rawCredential, CredentialFormat format) {
        try {
            return Result.success(SignedJWT.parse(rawCredential).getJWTClaimsSet().getClaims());
        } catch (ParseException e) {
            return Result.failure("A '%s' credential must be a JWT: %s".formatted(format, e.getMessage()));
        }
    }

    private Result<Map<String, Object>> jsonObject(String rawCredential, CredentialFormat format) {
        try {
            return Result.success(JSONObjectUtils.parse(rawCredential));
        } catch (ParseException e) {
            return Result.failure("A '%s' credential must be a JSON object: %s".formatted(format, e.getMessage()));
        }
    }

    private List<String> contexts(Map<String, Object> credential) {
        var context = credential.get(CONTEXT_PROPERTY);
        if (context instanceof Collection<?> contexts) {
            return contexts.stream().map(String::valueOf).toList();
        }
        return context == null ? List.of() : List.of(context.toString());
    }
}
