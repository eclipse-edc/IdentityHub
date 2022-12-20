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

import org.eclipse.edc.runtime.metamodel.annotation.ExtensionPoint;

/**
 * Registry interface for {@link CredentialEnvelopeVerifier} for verifying credentials in {@link org.eclipse.edc.identityhub.spi.credentials.model.CredentialEnvelope}
 * based on a given format.
 */
@ExtensionPoint
public interface CredentialVerifierRegistry {


    void register(String format, CredentialEnvelopeVerifier verifier);

    CredentialEnvelopeVerifier resolve(String format);

}
