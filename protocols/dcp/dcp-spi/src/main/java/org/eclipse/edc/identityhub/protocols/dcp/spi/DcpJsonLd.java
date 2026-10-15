/*
 *  Copyright (c) 2026 Metaform Systems, Inc.
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

package org.eclipse.edc.identityhub.protocols.dcp.spi;

import org.eclipse.edc.jsonld.spi.JsonLd;

/**
 * {@link JsonLd} service for DCP messages, which are received from, or sent by, other participants. Expanding a message
 * only resolves the contexts that were registered with {@link #registerCachedDocument}, and never loads a document from
 * anywhere else, so that a message cannot make the runtime access hosts or files that it references.
 */
public interface DcpJsonLd extends JsonLd {
}
