/*
 *  Copyright (c) 2022 Microsoft Corporation
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Microsoft Corporation - initial API and implementation
 *
 */

package org.eclipse.dataspaceconnector.identityhub.store;

import java.util.Collection;

/**
 * IdentityHubStore used to store data in an Identity Hub.
 * When used in a dataspace <a href="https://www.w3.org/TR/vc-data-model/">Verifiable Credentials</a> will be stored, but any other kind of data is supported as well.
 */
public interface IdentityHubStore {

    Collection<Object> getAll();

    void add(Object hubObject);
}
