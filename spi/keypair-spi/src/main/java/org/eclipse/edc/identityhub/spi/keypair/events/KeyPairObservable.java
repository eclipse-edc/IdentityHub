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

package org.eclipse.edc.identityhub.spi.keypair.events;

import org.eclipse.edc.spi.observe.Observable;

/**
 * Manages and invokes {@link KeyPairEventListener}s when a state change related to a key pair resource has happened.
 */
public interface KeyPairObservable extends Observable<KeyPairEventListener> {
}
