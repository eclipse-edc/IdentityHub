/*
 *  Copyright (c) 2025 Metaform Systems Inc.
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

package org.eclipse.edc.identityhub.credentialoffer.store.sql;

import org.assertj.core.api.Assertions;
import org.eclipse.edc.identityhub.spi.verifiablecredentials.model.CredentialOffer;
import org.eclipse.edc.identityhub.spi.verifiablecredentials.model.CredentialOfferStatus;
import org.eclipse.edc.identityhub.store.sql.credentialoffer.schema.CredentialOfferStoreStatements;
import org.eclipse.edc.identityhub.store.sql.credentialoffer.schema.SqlCredentialOfferStore;
import org.eclipse.edc.identityhub.store.sql.credentialoffer.schema.schema.postgres.PostgresDialectStatements;
import org.eclipse.edc.identityhub.verifiablecredentials.store.CredentialOfferStoreTestBase;
import org.eclipse.edc.json.JacksonTypeManager;
import org.eclipse.edc.junit.annotations.PostgresqlIntegrationTest;
import org.eclipse.edc.junit.testfixtures.TestUtils;
import org.eclipse.edc.sql.QueryExecutor;
import org.eclipse.edc.sql.testfixtures.PostgresqlStoreSetupExtension;
import org.eclipse.edc.sql.testfixtures.RowLeaseUtil;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.time.Duration;

import static org.eclipse.edc.junit.assertions.AbstractResultAssert.assertThat;

@PostgresqlIntegrationTest
@ExtendWith(PostgresqlStoreSetupExtension.class)
class SqlCredentialOfferStoreTest extends CredentialOfferStoreTestBase {

    private CredentialOfferStoreStatements statements;
    private SqlCredentialOfferStore store;
    private RowLeaseUtil leaseUtil;

    @BeforeEach
    void setup(PostgresqlStoreSetupExtension extension, QueryExecutor queryExecutor) {
        var typeManager = new JacksonTypeManager();

        statements = new PostgresDialectStatements(clock);

        store = new SqlCredentialOfferStore(extension.getDataSourceRegistry(), extension.getDatasourceName(),
                extension.getTransactionContext(), typeManager::getMapper, queryExecutor, statements, RUNTIME_ID, clock);

        leaseUtil = new RowLeaseUtil(extension.getTransactionContext(), extension::getConnection, statements, statements.getCredentialOffersTable(), clock);

        var schema = TestUtils.getResourceFileContentAsString("credential-offer-schema.sql");
        extension.runQuery(schema);
    }

    @AfterEach
    void tearDown(PostgresqlStoreSetupExtension extension) {
        extension.runQuery("DROP TABLE " + statements.getCredentialOffersTable() + " CASCADE");
    }

    @Test
    void schema_shouldAddLeaseAndVersionColumnsToExistingTable(PostgresqlStoreSetupExtension extension) {
        extension.runQuery("ALTER TABLE %s DROP COLUMN leased_by, DROP COLUMN leased_at, DROP COLUMN lease_duration, DROP COLUMN version"
                .formatted(statements.getCredentialOffersTable()));

        extension.runQuery(TestUtils.getResourceFileContentAsString("credential-offer-schema.sql"));

        var offer = CredentialOffer.Builder.newInstance()
                .id("offer-id")
                .state(CredentialOfferStatus.RECEIVED.code())
                .participantContextId("test-participant")
                .issuer("did:web:issuer")
                .build();
        assertThat(store.save(offer)).isSucceeded();
        Assertions.assertThat(store.nextNotLeased(1)).extracting(CredentialOffer::getId).containsExactly(offer.getId());
        Assertions.assertThat(leaseUtil.isLeased(offer.getId(), RUNTIME_ID)).isTrue();
    }

    @Override
    protected SqlCredentialOfferStore getStore() {
        return store;
    }

    @Override
    protected boolean isLeasedBy(String issuanceId, String owner) {
        return leaseUtil.isLeased(issuanceId, owner);
    }

    @Override
    protected void leaseEntity(String issuanceId, String owner, Duration duration) {
        leaseUtil.leaseEntity(issuanceId, owner, duration);
    }
}