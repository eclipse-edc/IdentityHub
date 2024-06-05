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

package org.eclipse.edc.identityhub.api;

public interface Versions {
    String UNSTABLE = "/v1alpha";
    // Once /v1 has become stable, there will be a String STABLE = "/v1"
    // Once /v2 has become stable, there will be a String DEPRECATED = "/v1", and STABLE = "/v2"
}
