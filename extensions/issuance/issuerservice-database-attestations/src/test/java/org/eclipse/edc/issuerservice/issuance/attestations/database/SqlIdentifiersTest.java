/*
 *  Copyright (c) 2026 Metaform Systems Inc.
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Metaform Systems Inc. - initial API and implementation
 *
 */

package org.eclipse.edc.issuerservice.issuance.attestations.database;

import org.eclipse.edc.issuerservice.issuance.database.SqlIdentifiers;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

class SqlIdentifiersTest {

    @ParameterizedTest
    @ValueSource(strings = {"membership", "Membership_Attestations", "_tbl", "tbl2", "public.membership", "my_schema._tbl"})
    void isValidTableName_valid(String name) {
        assertThat(SqlIdentifiers.isValidTableName(name)).isTrue();
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"1tbl", "tbl-name", "tbl name", "tbl;", "\"tbl\"", "a.b.c", ".tbl", "tbl.", "tbl--", "tbl/*x*/", "täble"})
    void isValidTableName_invalid(String name) {
        assertThat(SqlIdentifiers.isValidTableName(name)).isFalse();
    }

    @ParameterizedTest
    @ValueSource(strings = {"holder_id", "HolderId", "_id", "id2"})
    void isValidColumnName_valid(String name) {
        assertThat(SqlIdentifiers.isValidColumnName(name)).isTrue();
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"1id", "holder-id", "holder id", "t.holder_id", "holder_id,id", "*"})
    void isValidColumnName_invalid(String name) {
        assertThat(SqlIdentifiers.isValidColumnName(name)).isFalse();
    }

    @ParameterizedTest
    @ValueSource(ints = {63, 64})
    void isValidColumnName_maxLength(int length) {
        assertThat(SqlIdentifiers.isValidColumnName("a".repeat(length))).isEqualTo(length <= 63);
    }
}
