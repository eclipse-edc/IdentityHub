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

package org.eclipse.edc.identityhub.api.keypair.validation;

import org.eclipse.edc.iam.did.spi.document.DidConstants;
import org.eclipse.edc.identityhub.spi.participantcontext.model.KeyDescriptor;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.util.string.StringUtils;
import org.eclipse.edc.validator.spi.ValidationResult;
import org.eclipse.edc.validator.spi.Validator;

import java.util.Objects;
import java.util.stream.Stream;

import static java.lang.String.format;
import static org.eclipse.edc.validator.spi.ValidationResult.failure;
import static org.eclipse.edc.validator.spi.ValidationResult.success;
import static org.eclipse.edc.validator.spi.Violation.violation;

/**
 * Validates a {@link KeyDescriptor} by checking the following rules:
 * <ul>
 *     <li>the {@code keyId} must not be null</li>
 *     <li>the {@code privateKeyAlias} must not be null</li>
 *     <li>not all of {@code publicKeyPem}, {@code publicKeyJwk} and {@code keyGeneratorParams} must be null</li>
 *     <li>not both  {@code publicKeyPem} and {@code publicKeyJwk} must be specified</li>
 *     <li>if {@code keyGeneratorParams} are specified, {@code publicKeyPem} and {@code publicKeyJwk} must be null</li>
 * </ul>
 */
public class KeyDescriptorValidator implements Validator<KeyDescriptor> {

    private final Monitor monitor;

    public KeyDescriptorValidator(Monitor monitor) {
        this.monitor = monitor;
    }

    @Override
    public ValidationResult validate(KeyDescriptor input) {
        if (input == null) {
            return failure(violation("input was null", "."));
        }

        if (StringUtils.isNullOrBlank(input.getKeyId())) {
            return failure(violation("keyId cannot be null.", "keyId"));
        }

        if (!DidConstants.ALLOWED_VERIFICATION_TYPES.contains(input.getType())) {
            monitor.warning(format("Provided type %s is not supported.", input.getType()));
        }

        if (StringUtils.isNullOrBlank(input.getPrivateKeyAlias())) {
            return failure(violation("privateKeyAlias cannot be null.", "privateKeyAlias"));
        }

        // either the public key or the key generator params are provided
        if (allNull(input.getKeyGeneratorParams(), input.getPublicKeyJwk(), input.getPublicKeyPem())) {
            return failure(violation("Either the public key is specified (PEM or JWK), or the generator parameters are provided.",
                    "publicKeyJwk, publicKeyPem, keyGeneratorParams"));
        }

        if (input.getPublicKeyJwk() != null && input.getPublicKeyPem() != null) {
            return failure(violation("The public key must either be provided in PEM or in JWK format, not both.", "publicKeyPem, publicKeyJwk"));
        }

        if (Stream.of(input.getPublicKeyJwk(), input.getPublicKeyPem()).anyMatch(Objects::nonNull) && input.getKeyGeneratorParams() != null) {
            return failure(violation("Either the public key is specified (PEM or JWK), or the generator params are provided, not both.", "keyGeneratorPArams"));
        }

        return success();
    }

    private boolean allNull(Object... objects) {
        return Stream.of(objects).allMatch(Objects::isNull);
    }

}
