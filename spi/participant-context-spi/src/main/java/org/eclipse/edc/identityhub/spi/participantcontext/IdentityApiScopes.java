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
 * Well-known scope strings for the Identity API. The grammar is {@code identity-api:[resource:]action}; endpoints
 * declare per-resource scopes (e.g. {@code identity-api:dids:write}) as string literals, while the constants below are
 * the coarse, resource-wildcard tier and the admin elevation flag.
 */
public interface IdentityApiScopes {

    /**
     * The namespace (scope prefix) for all Identity API scopes.
     */
    String NAMESPACE = "identity-api";

    /**
     * Read access to (any) resource: shorthand for {@code identity-api:*:read}.
     */
    String READ = NAMESPACE + ":read";

    /**
     * Write access to (any) resource: shorthand for {@code identity-api:*:write}. Implies {@link #READ}.
     */
    String WRITE = NAMESPACE + ":write";

    /**
     * Cross-participant elevation / superuser access: shorthand for {@code identity-api:*:admin}. Implies {@link #WRITE}
     * and {@link #READ}, and additionally bypasses the resource-ownership check.
     */
    String ADMIN = NAMESPACE + ":admin";
}
