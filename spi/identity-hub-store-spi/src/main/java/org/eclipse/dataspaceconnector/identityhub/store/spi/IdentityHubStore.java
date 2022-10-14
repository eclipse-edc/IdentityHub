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

package org.eclipse.dataspaceconnector.identityhub.store.spi;

import java.util.Collection;

/**
 * IdentityHubStore used to store data in an Identity Hub.
 */
public interface IdentityHubStore {

    Collection<byte[]> getAll();

    void add(byte[] hubObject);
}
