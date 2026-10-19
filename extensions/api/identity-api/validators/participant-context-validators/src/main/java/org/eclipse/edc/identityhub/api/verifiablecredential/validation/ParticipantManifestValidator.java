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
 *       Metaform Systems, Inc. - initial API and implementation
 *
 */

package org.eclipse.edc.identityhub.api.verifiablecredential.validation;

import org.eclipse.edc.identityhub.api.keypair.validation.KeyDescriptorValidator;
import org.eclipse.edc.identityhub.spi.participantcontext.model.ParticipantManifest;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.util.string.StringUtils;
import org.eclipse.edc.validator.spi.ValidationResult;
import org.eclipse.edc.validator.spi.Validator;

import java.util.regex.Pattern;

import static org.eclipse.edc.validator.spi.ValidationResult.failure;
import static org.eclipse.edc.validator.spi.ValidationResult.success;
import static org.eclipse.edc.validator.spi.Violation.violation;

public class ParticipantManifestValidator implements Validator<ParticipantManifest> {
    private static final String ID_CHARS = "(?:[A-Za-z0-9._-]|%[0-9A-Fa-f]{2})";
    /**
     * DID syntax as per <a href="https://www.w3.org/TR/did-1.0/#did-syntax">DID Core</a>
     */
    private static final Pattern DID_PATTERN = Pattern.compile("did:[a-z0-9]+:(?:%1$s*:)*%1$s+".formatted(ID_CHARS));
    /**
     * did:web identifiers must not contain empty segments, see <a href="https://w3c-ccg.github.io/did-method-web/#method-specific-identifier">did:web</a>
     */
    private static final Pattern DID_WEB_PATTERN = Pattern.compile("did:web:%1$s+(?::%1$s+)*".formatted(ID_CHARS));
    private final KeyDescriptorValidator keyDescriptorValidator;

    public ParticipantManifestValidator(Monitor monitor) {
        this.keyDescriptorValidator = new KeyDescriptorValidator(monitor);
    }

    @Override
    public ValidationResult validate(ParticipantManifest input) {
        if (input == null) {
            return failure(violation("input was null.", "."));
        }

        if (input.getKeys() == null) {
            return failure(violation("key descriptor cannot be null.", "keys"));
        }
        if (input.getKeys().isEmpty()) {
            return failure(violation("key descriptor cannot be empty.", "keys"));
        }
        if (StringUtils.isNullOrBlank(input.getParticipantContextId())) {
            return failure(violation("participantContextId cannot be null or empty.", "participantContextId"));
        }
        if (StringUtils.isNullOrBlank(input.getDid())) {
            return failure(violation("DID cannot be null or empty.", "did"));
        }
        if (!isValidDid(input.getDid())) {
            return failure(violation("DID '%s' is not a valid DID.".formatted(input.getDid()), "did"));
        }

        var mergedResult = input.getKeys().stream()
                .map(keyDescriptorValidator::validate)
                .reduce(ValidationResult::merge)
                .orElse(success());
        if (mergedResult.failed()) {
            return failure(violation("key descriptor is invalid: %s".formatted(mergedResult.getFailureDetail()), "key"));
        }


        return success();
    }

    private static boolean isValidDid(String did) {
        var pattern = did.startsWith("did:web:") ? DID_WEB_PATTERN : DID_PATTERN;
        return pattern.matcher(did).matches();
    }
}
