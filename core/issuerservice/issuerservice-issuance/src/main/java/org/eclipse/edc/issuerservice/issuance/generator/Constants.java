/*
 *  Copyright (c) 2026 Metaform Systems, Inc.
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

package org.eclipse.edc.issuerservice.issuance.generator;

public interface Constants {
    String VERIFIABLE_CREDENTIAL_CLAIM = "vc";
    String CREDENTIAL_SUBJECT = "credentialSubject";
    String CREDENTIAL_STATUS = "credentialStatus";
    String VERIFIABLE_CREDENTIAL = "VerifiableCredential";
    String TYPE = "type";
    String VALID_FROM = "validFrom";
    String ID = "id";
    String ISSUER = "issuer";
    String W3C_CREDENTIALS_URL_V2 = "https://www.w3.org/ns/credentials/v2";
}
