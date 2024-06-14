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
 *       Metaform Systems, Inc. - initial API and implementation
 *
 */

package org.eclipse.edc.identityhub.accesstoken.verification;

public interface AccessTokenConstants {
    String DCP_SELF_ISSUED_TOKEN_CONTEXT = "dcp-si";
    String DCP_ACCESS_TOKEN_CONTEXT = "dcp-access-token";
    String TOKEN_CLAIM = "token";
    String ACCESS_TOKEN_SCOPE_CLAIM = "scope";
}
