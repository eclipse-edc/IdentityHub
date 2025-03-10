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

package org.eclipse.edc.identityhub.tests.dcp;

public class TestData {

    public static final String ISSUER_RUNTIME_NAME = "issuerservice";
    public static final String ISSUER_RUNTIME_ID = "issuerservice";

    public static final String[] ISSUER_RUNTIME_SQL_MODULES = new String[]{":dist:bom:issuerservice-bom", ":dist:bom:issuerservice-feature-sql-bom"};
    public static final String[] ISSUER_RUNTIME_MEM_MODULES = new String[]{":dist:bom:issuerservice-bom"};


    public static final String IH_RUNTIME_NAME = "identity-hub";
    public static final String IH_RUNTIME_ID = "identity-hub";

    public static final String[] IH_RUNTIME_SQL_MODULES = new String[]{":dist:bom:identityhub-bom", ":dist:bom:identityhub-feature-sql-bom"};
    public static final String[] IH_RUNTIME_MEM_MODULES = new String[]{":dist:bom:identityhub-bom"};
}
