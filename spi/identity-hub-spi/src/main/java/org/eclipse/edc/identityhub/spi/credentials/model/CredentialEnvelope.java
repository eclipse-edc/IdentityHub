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

package org.eclipse.edc.identityhub.spi.credentials.model;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.eclipse.edc.spi.result.Result;

/**
 * The {@link CredentialEnvelope} it's used to wrap an implementation of verifiable credential with a given format.
 */
public interface CredentialEnvelope {

    /**
     * Returns the Media type that implementor of {@link CredentialEnvelope} is able to validate.
     */
    String format();

    /**
     * Convert the content of {@link CredentialEnvelope} to {@link VerifiableCredential}
     *
     * @param mapper The json mapper.
     * @return The result of the conversion process
     */

    Result<VerifiableCredential> toVerifiableCredential(ObjectMapper mapper);
}
