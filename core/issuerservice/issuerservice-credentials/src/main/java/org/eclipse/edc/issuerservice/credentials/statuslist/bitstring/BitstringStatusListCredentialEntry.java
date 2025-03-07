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

package org.eclipse.edc.issuerservice.credentials.statuslist.bitstring;

import org.eclipse.edc.iam.verifiablecredentials.spi.model.CredentialStatus;
import org.eclipse.edc.identityhub.spi.verifiablecredentials.model.VerifiableCredentialResource;
import org.eclipse.edc.issuerservice.spi.credentials.statuslist.StatusListCredentialEntry;

import java.util.Map;
import java.util.UUID;

import static org.eclipse.edc.issuerservice.credentials.statuslist.bitstring.BitstringConstants.BITSTRING_STATUS_LIST_ENTRY;

public record BitstringStatusListCredentialEntry(int statusListIndex, VerifiableCredentialResource statusListCredential,
                                                 String credentialUrl) implements StatusListCredentialEntry {

    @Override
    public CredentialStatus createCredentialStatus() {
        return new CredentialStatus(UUID.randomUUID().toString(), BITSTRING_STATUS_LIST_ENTRY,
                Map.of(
                        "statusPurpose", statusListCredential().getVerifiableCredential().credential().getCredentialSubject().get(0).getClaim("", "statusPurpose"),
                        "statusListIndex", statusListIndex(),
                        "statusListCredential", credentialUrl()
                ));
    }
}
