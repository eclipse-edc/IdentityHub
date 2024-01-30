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

package org.eclipse.edc.identityhub.participantcontext;

import org.eclipse.edc.identityhub.spi.events.participant.ParticipantContextListener;
import org.eclipse.edc.identityhub.spi.events.participant.ParticipantContextObservable;
import org.eclipse.edc.spi.observe.ObservableImpl;

public class ParticipantContextObservableImpl extends ObservableImpl<ParticipantContextListener> implements ParticipantContextObservable {
}
