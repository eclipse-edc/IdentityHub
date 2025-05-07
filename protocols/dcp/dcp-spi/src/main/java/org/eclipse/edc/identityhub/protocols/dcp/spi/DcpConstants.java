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

package org.eclipse.edc.identityhub.protocols.dcp.spi;

import org.eclipse.edc.jsonld.spi.JsonLdNamespace;

public interface DcpConstants {

    String V_1_0 = "v1.0";
    @Deprecated(since = "0.12.0")
    String V_0_8 = "v0.8";
    String DCP_SCOPE_SEPARATOR = ":";
    String DCP_SCOPE_PREFIX = "dcp-api";
    @Deprecated(since = "0.12.0")
    String DCP_SCOPE_V_0_8 = DCP_SCOPE_PREFIX + DCP_SCOPE_SEPARATOR + V_0_8;
    String DCP_SCOPE_V_1_0 = DCP_SCOPE_PREFIX + DCP_SCOPE_SEPARATOR + V_1_0;
    // URL where the DCP Specification resides. Could be used for externalDocs properties in Swagger
    String DCP_SPECIFICATION_URL = "https://eclipse-dataspace-dcp.github.io/decentralized-claims-protocol";
    JsonLdNamespace CREDENTIALS_NAMESPACE_W3C = new JsonLdNamespace("https://www.w3.org/2018/credentials#");
}
