/*
 *  Copyright (c) 2024 Metaform Systems, Inc.
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Bayerische Motoren Werke Aktiengesellschaft (BMW AG) - initial API and implementation
 *
 */

package org.eclipse.edc.identityhub.spi.verification;

public interface SelfIssuedTokenConstants {
    String DCP_PRESENTATION_SELF_ISSUED_TOKEN_CONTEXT = "dcp-si";
    String DCP_PRESENTATION_ACCESS_TOKEN_CONTEXT = "dcp-access-token";
    String DCP_ISSUANCE_SELF_ISSUED_TOKEN_CONTEXT = "dcp-issuance-si";
    String TOKEN_CLAIM = "token";
    String ACCESS_TOKEN_SCOPE_CLAIM = "scope";
}
