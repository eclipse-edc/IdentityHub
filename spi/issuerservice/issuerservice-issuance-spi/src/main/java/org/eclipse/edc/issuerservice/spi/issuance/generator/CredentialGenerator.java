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

package org.eclipse.edc.issuerservice.spi.issuance.generator;

import org.eclipse.edc.iam.verifiablecredentials.spi.model.CredentialFormat;
import org.eclipse.edc.iam.verifiablecredentials.spi.model.VerifiableCredential;
import org.eclipse.edc.iam.verifiablecredentials.spi.model.VerifiableCredentialContainer;
import org.eclipse.edc.issuerservice.spi.issuance.model.CredentialDefinition;
import org.eclipse.edc.runtime.metamodel.annotation.ExtensionPoint;
import org.eclipse.edc.spi.result.Result;

import java.util.Map;

/**
 * A generator for credentials based on a given definition.
 * A {@link CredentialGenerator} should be associated in the {@link CredentialGeneratorRegistry} to a
 * particular {@link CredentialFormat}
 */
@ExtensionPoint
public interface CredentialGenerator {
    /**
     * Generates (and signs) a credential based on the given definition and claims
     *
     * @param definition      the definition of the credential
     * @param privateKeyAlias the alias of the private key to use for signing
     * @param publicKeyId     the ID of the public key to use for signing
     * @param issuerId        the ID of the issuer
     * @param participantId   the ID of the participant
     * @param claims          the claims to include in the credential
     * @return the generated {@link VerifiableCredentialContainer} including the original credential, its serialized and signed form and the format
     */
    Result<VerifiableCredentialContainer> generateCredential(CredentialDefinition definition, String privateKeyAlias, String publicKeyId, String issuerId, String participantId, Map<String, Object> claims);


    /**
     * Signs an input {@link VerifiableCredential} with the given private key.
     *
     * @param credential      the input credential
     * @param privateKeyAlias The alias of the private they that is expected to be found in the {@link org.eclipse.edc.spi.security.Vault}
     * @param publicKeyId     the ID of the public key. Relevant for adding verification material to the signed representation.
     * @return a String representing the serialized and signed credential, for example a JWT token, a Json-LD structure, etc.
     */
    Result<String> signCredential(VerifiableCredential credential, String privateKeyAlias, String publicKeyId);

}
