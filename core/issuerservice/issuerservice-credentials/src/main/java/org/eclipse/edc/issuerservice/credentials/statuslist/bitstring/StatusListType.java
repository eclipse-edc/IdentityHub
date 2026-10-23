/*
 *  Copyright (c) 2026 Metaform Systems Inc.
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Metaform Systems Inc. - initial API and implementation
 *
 */

package org.eclipse.edc.issuerservice.credentials.statuslist.bitstring;

import org.eclipse.edc.iam.verifiablecredentials.spi.model.CredentialFormat;
import org.eclipse.edc.iam.verifiablecredentials.spi.model.DataModelVersion;
import org.eclipse.edc.iam.verifiablecredentials.spi.model.revocation.BitString;
import org.eclipse.edc.iam.verifiablecredentials.spi.model.revocation.bitstringstatuslist.BitstringStatusListStatus;
import org.eclipse.edc.iam.verifiablecredentials.spi.model.revocation.statuslist2021.StatusList2021Status;
import org.eclipse.edc.spi.result.Result;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.eclipse.edc.iam.verifiablecredentials.spi.VcConstants.STATUSLIST_2021_URL;
import static org.eclipse.edc.iam.verifiablecredentials.spi.VcConstants.VC_PREFIX_V2;
import static org.eclipse.edc.iam.verifiablecredentials.spi.VcConstants.W3C_CREDENTIALS_URL;
import static org.eclipse.edc.iam.verifiablecredentials.spi.model.revocation.bitstringstatuslist.BitstringStatusListCredential.BITSTRING_STATUSLIST_CREDENTIAL;
import static org.eclipse.edc.iam.verifiablecredentials.spi.model.revocation.statuslist2021.StatusList2021Credential.STATUSLIST_2021_CREDENTIAL;

/**
 * The kinds of bitstring based status lists an issuer can maintain. The DCP profile the issuer issues credentials under
 * names the kind: {@code vc11-sl2021/jwt} a <a href="https://www.w3.org/TR/2023/WD-vc-status-list-20230427/">StatusList2021</a>,
 * {@code vc20-bssl/jwt} a <a href="https://www.w3.org/TR/vc-bitstring-status-list/">Bitstring Status List</a>. The status
 * list credential uses the data model and format of the credentials of that profile.
 */
public enum StatusListType {

    STATUS_LIST_2021(CredentialFormat.VC1_0_JWT, DataModelVersion.V_1_1, List.of(W3C_CREDENTIALS_URL, STATUSLIST_2021_URL),
            STATUSLIST_2021_CREDENTIAL, "StatusList2021", StatusList2021Status.TYPE),
    BITSTRING_STATUS_LIST(CredentialFormat.VC2_0_JOSE, DataModelVersion.V_2_0, List.of(VC_PREFIX_V2),
            BITSTRING_STATUSLIST_CREDENTIAL, "BitstringStatusList", BitstringStatusListStatus.TYPE);

    private final CredentialFormat format;
    private final DataModelVersion dataModelVersion;
    private final List<String> contexts;
    private final String credentialType;
    private final String subjectType;
    private final String entryType;

    StatusListType(CredentialFormat format, DataModelVersion dataModelVersion, List<String> contexts, String credentialType, String subjectType, String entryType) {
        this.format = format;
        this.dataModelVersion = dataModelVersion;
        this.contexts = contexts;
        this.credentialType = credentialType;
        this.subjectType = subjectType;
        this.entryType = entryType;
    }

    /**
     * The status list whose entries have the given type, e.g. the status list type of a DCP profile, if there is one.
     */
    public static Optional<StatusListType> forEntryType(String entryType) {
        return Arrays.stream(values()).filter(type -> type.entryType.equals(entryType)).findFirst();
    }

    /**
     * The status list whose credential subject has the given type, if there is one.
     */
    public static Optional<StatusListType> forSubjectType(Object subjectType) {
        return Arrays.stream(values()).filter(type -> type.subjectType.equals(subjectType)).findFirst();
    }

    /**
     * The format of the status list credential.
     */
    public CredentialFormat format() {
        return format;
    }

    public DataModelVersion dataModelVersion() {
        return dataModelVersion;
    }

    public List<String> contexts() {
        return contexts;
    }

    /**
     * The type of the status list credential, besides {@code VerifiableCredential}.
     */
    public String credentialType() {
        return credentialType;
    }

    /**
     * The type of the status list credential's subject, which tells the status lists of the different kinds apart.
     */
    public String subjectType() {
        return subjectType;
    }

    /**
     * The type of the {@code credentialStatus} entry that a holder credential gets.
     */
    public String entryType() {
        return entryType;
    }

    /**
     * Compresses and encodes a bitstring as the {@code encodedList} of the status list credential: StatusList2021 requires
     * it to be base64 encoded, the Bitstring Status List to be multibase encoded.
     */
    public Result<String> encode(BitString bitString) {
        var writer = BitString.Writer.newInstance();
        return this == STATUS_LIST_2021 ? writer.write(bitString) : writer.writeMultibase(bitString);
    }
}
