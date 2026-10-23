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

import org.eclipse.edc.iam.verifiablecredentials.spi.model.CredentialSubject;
import org.eclipse.edc.iam.verifiablecredentials.spi.model.revocation.BitString;
import org.eclipse.edc.identityhub.spi.verifiablecredentials.model.VerifiableCredentialResource;
import org.eclipse.edc.issuerservice.spi.credentials.statuslist.StatusListInfo;
import org.eclipse.edc.spi.result.Result;

import static org.eclipse.edc.iam.verifiablecredentials.spi.model.revocation.bitstringstatuslist.BitstringStatusListCredential.BITSTRING_ENCODED_LIST_LITERAL;
import static org.eclipse.edc.spi.result.Result.failure;
import static org.eclipse.edc.spi.result.Result.success;


/**
 * {@link StatusListInfo} object specific for bitstring based status list credentials, c.f. {@link StatusListType}.
 * <p>
 * Note that at this time, {@code statusSize} and {@code statusMessage} are not supported, that means, the only valid
 * status values are "set" (1) and "not set" (0).
 *
 * @param type                 the type of the status list credential
 * @param index                the statusIndex of the credential in question
 * @param statusListCredential the {@link VerifiableCredentialResource} of the status list credential
 */
record BitstringStatusInfo(StatusListType type, int index, VerifiableCredentialResource statusListCredential) implements StatusListInfo {

    private static final String STATUS_PURPOSE = "statusPurpose";

    /**
     * The status field of the holder's credential, e.g. "revocation".
     *
     * @return a string indicating the status, or null if the status is not set.
     */
    @Override
    public Result<String> getStatus() {
        var uncompressedBitString = decode();
        return uncompressedBitString.map(bs -> bs.get(index) ? statusPurpose() : null);
    }

    /**
     * sets the status bit in the bitstring of the status list credential
     *
     * @return the new compressed, encoded bitstring
     */
    @Override
    public Result<Void> setStatus(boolean status) {
        var uncompressedBitString = decode();
        // set "revoked" bit
        return uncompressedBitString.compose(bs -> {
            bs.set(index, status);
            var res = type.encode(bs);
            // update bitstring in credential status
            res.onSuccess(newBitString -> subject()
                    .toBuilder() //modifies the original instance
                    .claim(BITSTRING_ENCODED_LIST_LITERAL, newBitString)
                    .build());
            return res.mapEmpty();
        });
    }

    private CredentialSubject subject() {
        return statusListCredential.getVerifiableCredential().credential().getCredentialSubject().get(0);
    }

    private String statusPurpose() {
        var purpose = subject().getClaim("", STATUS_PURPOSE);
        return purpose != null ? purpose.toString() : null;
    }

    private Result<BitString> decode() {
        var encodedList = subject().getClaim("", BITSTRING_ENCODED_LIST_LITERAL);
        if (encodedList == null) {
            return failure("Status list credential '%s' has no encoded list".formatted(statusListCredential.getId()));
        }
        var decompressionResult = BitString.Parser.newInstance().parse(encodedList.toString());
        if (decompressionResult.failed()) {
            return failure("Failed to decode compressed BitString: '%s'".formatted(decompressionResult.getFailureDetail()));
        }
        return success(decompressionResult.getContent());
    }
}
