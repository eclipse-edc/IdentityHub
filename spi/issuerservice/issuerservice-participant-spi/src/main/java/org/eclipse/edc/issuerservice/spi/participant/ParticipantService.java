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

package org.eclipse.edc.issuerservice.spi.participant;

import org.eclipse.edc.issuerservice.spi.participant.model.Participant;
import org.eclipse.edc.spi.query.QuerySpec;
import org.eclipse.edc.spi.result.ServiceResult;

import java.util.Collection;

public interface ParticipantService {

    ServiceResult<Void> createParticipant(Participant participant);

    ServiceResult<Void> deleteParticipant(String participantId);

    ServiceResult<Void> updateParticipant(Participant participant);

    ServiceResult<Collection<Participant>> queryParticipants(QuerySpec querySpec);

    ServiceResult<Participant> findById(String participantId);
}
