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


/**
 * This module declares several service interfaces and extension points that are required to host, publish and store
 * DID documents.
 */
@Spi(value = "Identity Hub DID services")
package org.eclipse.edc.identityhub.spi.did;

import org.eclipse.edc.runtime.metamodel.annotation.Spi;
