/*
 *  Copyright (c) 2022 Microsoft Corporation
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Microsoft Corporation - initial API and implementation
 *
 */

package org.eclipse.edc.identityhub.spi.credentials.verifier;

import org.eclipse.edc.iam.did.spi.document.DidDocument;
import org.eclipse.edc.identityhub.spi.credentials.model.CredentialEnvelope;
import org.eclipse.edc.spi.result.Result;

import java.util.Map;

/**
 * Abstraction over the verification process on verifiable credential carried in a {@link CredentialEnvelope}
 */
public interface CredentialEnvelopeVerifier<T extends CredentialEnvelope> {

    Result<Map.Entry<String, Object>> verify(T verifiableCredentials, DidDocument didDocument);
}
