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

/**
 * Generates a random string, e.g. a UUID. Actual production implementations sould be more sophisticated, e.g. using seeds/salts and {@link java.security.SecureRandom}
 */
@FunctionalInterface
public interface RandomStringGenerator {
    String generate();
}
