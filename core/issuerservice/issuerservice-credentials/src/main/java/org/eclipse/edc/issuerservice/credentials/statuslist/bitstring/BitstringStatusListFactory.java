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
import org.eclipse.edc.identityhub.spi.verifiablecredentials.store.CredentialStore;
import org.eclipse.edc.issuerservice.spi.credentials.statuslist.StatusListInfo;
import org.eclipse.edc.issuerservice.spi.credentials.statuslist.StatusListInfoFactory;
import org.eclipse.edc.spi.result.ServiceResult;

import static org.eclipse.edc.iam.verifiablecredentials.spi.VcConstants.BITSTRING_STATUS_LIST_PREFIX;
import static org.eclipse.edc.iam.verifiablecredentials.spi.model.revocation.bitstringstatuslist.BitstringStatusListStatus.BITSTRING_STATUS_LIST_CREDENTIAL_LITERAL;
import static org.eclipse.edc.iam.verifiablecredentials.spi.model.revocation.bitstringstatuslist.BitstringStatusListStatus.BITSTRING_STATUS_LIST_INDEX_LITERAL;
import static org.eclipse.edc.spi.result.ServiceResult.success;

public class BitstringStatusListFactory implements StatusListInfoFactory {
    private final CredentialStore credentialStore;

    public BitstringStatusListFactory(CredentialStore credentialStore) {
        this.credentialStore = credentialStore;
    }

    @Override
    public ServiceResult<StatusListInfo> create(CredentialStatus credentialStatus) {

        var statusListCredentialId = credentialStatus.getProperty(BITSTRING_STATUS_LIST_PREFIX, BITSTRING_STATUS_LIST_CREDENTIAL_LITERAL);
        var index = credentialStatus.getProperty(BITSTRING_STATUS_LIST_PREFIX, BITSTRING_STATUS_LIST_INDEX_LITERAL);

        if (statusListCredentialId == null) {
            return ServiceResult.unexpected("The credential status with ID '%s' is invalid, the '%s' field is missing".formatted(credentialStatus.id(), BITSTRING_STATUS_LIST_CREDENTIAL_LITERAL));

        }
        if (index == null) {
            return ServiceResult.unexpected("The credential status with ID '%s' is invalid, the '%s' field is missing".formatted(credentialStatus.id(), BITSTRING_STATUS_LIST_INDEX_LITERAL));
        }

        var ix = Integer.parseInt(index.toString());

        var result = credentialStore.findById(statusListCredentialId.toString());
        return result.succeeded()
                ? success(new BitstringStatusInfo(ix, result.getContent()))
                : ServiceResult.fromFailure(result);
    }
}
