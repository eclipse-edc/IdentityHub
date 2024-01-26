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

import org.eclipse.edc.identityhub.spi.model.ParticipantResource;
import org.eclipse.edc.spi.result.ServiceResult;

import java.security.Principal;
import java.util.Map;
import java.util.function.Function;

public interface AuthorizationService {
    ServiceResult<Void> isAuthorized(Principal user, String resourceId, Class<?> resourceClass);

    Map<Class<?>, Function<String, ParticipantResource>> getAuthorizationCheckFunctions();
}
