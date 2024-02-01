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

package org.eclipse.edc.identityhub.spi.authentication;

import java.security.Principal;
import java.util.List;

/**
 * A user is a representation of the security principal that executes a request against an HTTP API. It must be resolvable during request pre-matching.
 */
public interface ServicePrincipal extends Principal {

    String ROLE_ADMIN = "admin";

    /**
     * The "principal", e.g. the user ID, or a unique service identifier.
     */
    String getPrincipal();

    /**
     * The credential of the user, e.g. an API token or a password.
     */
    String getCredential();

    /**
     * The roles that this user possesses. May be empty, never null.
     */
    List<String> getRoles();

    @Override
    default String getName() {
        return getPrincipal();
    }
}
