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

package org.eclipse.edc.identityhub.spi.participantcontext.model;

/**
 * The state a {@link IdentityHubParticipantContext} entry is in.
 */
public enum ParticipantContextState {
    /**
     * The {@link IdentityHubParticipantContext} was created in the database, but is not yet operational.
     */
    CREATED,
    /**
     * The {@link IdentityHubParticipantContext} is operational and can be used.
     */
    ACTIVATED,
    /**
     * The {@link IdentityHubParticipantContext} is disabled and can not be used currently.
     */
    DEACTIVATED
}
