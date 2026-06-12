/*
 *  Copyright (c) 2025 Metaform Systems, Inc.
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

package org.eclipse.edc.identityhub.spi.participantcontext;

/**
 * Well-known scope strings for the Issuer Admin API. The grammar is {@code issuer-admin-api:[resource:]action};
 * endpoints declare per-resource scopes (e.g. {@code issuer-admin-api:holders:write}) as string literals, while the
 * constants below are the coarse, resource-wildcard tier and the admin elevation flag.
 */
public interface IssuerAdminApiScopes {

    /**
     * The namespace (scope prefix) for all Issuer Admin API scopes.
     */
    String NAMESPACE = "issuer-admin-api";

    /**
     * Read access to (any) resource: shorthand for {@code issuer-admin-api:*:read}.
     */
    String READ = NAMESPACE + ":read";

    /**
     * Write access to (any) resource: shorthand for {@code issuer-admin-api:*:write}. Implies {@link #READ}.
     */
    String WRITE = NAMESPACE + ":write";

    /**
     * Cross-participant elevation / superuser access: shorthand for {@code issuer-admin-api:*:admin}. Implies
     * {@link #WRITE} and {@link #READ}, and additionally bypasses the resource-ownership check.
     */
    String ADMIN = NAMESPACE + ":admin";
}
