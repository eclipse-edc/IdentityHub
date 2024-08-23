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

package org.eclipse.edc.identityhub.spi;

public interface IdentityHubApiContext {
    String IDENTITY = "identity";
    String IH_DID = "did";
    String PRESENTATION = "presentation";
    @Deprecated(since = "0.9.0")
    String RESOLUTION = "resolution";
}
