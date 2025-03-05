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

package org.eclipse.edc.issuerservice.api.admin.credentials.v1.unstable.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.eclipse.edc.iam.verifiablecredentials.spi.model.CredentialFormat;
import org.eclipse.edc.iam.verifiablecredentials.spi.model.VerifiableCredential;

/**
 * Returns metadata about a verifiable credential
 *
 * @param format               The {@link CredentialFormat} of the credential
 * @param verifiableCredential Structured metadata about the VC, not the actual VC. This will not contain the proof!
 */
public record VerifiableCredentialDto(@JsonProperty("participantContextId") String participantContextId,
                                      @JsonProperty("format") CredentialFormat format,
                                      @JsonProperty("credential") VerifiableCredential verifiableCredential) {

}
