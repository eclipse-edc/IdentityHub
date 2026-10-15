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

package org.eclipse.edc.identityhub.credentialrequest.store.sql;

import org.assertj.core.api.Assertions;
import org.eclipse.edc.identityhub.credential.request.test.HolderCredentialRequestStoreTestBase;
import org.eclipse.edc.identityhub.spi.credential.request.model.HolderCredentialRequest;
import org.eclipse.edc.identityhub.store.sql.credentialrequest.schema.HolderCredentialRequestStoreStatements;
import org.eclipse.edc.identityhub.store.sql.credentialrequest.schema.SqlHolderCredentialRequestStore;
import org.eclipse.edc.identityhub.store.sql.credentialrequest.schema.schema.postgres.PostgresDialectStatements;
import org.eclipse.edc.json.JacksonTypeManager;
import org.eclipse.edc.junit.annotations.PostgresqlIntegrationTest;
import org.eclipse.edc.junit.testfixtures.TestUtils;
import org.eclipse.edc.spi.result.StoreFailure;
import org.eclipse.edc.sql.QueryExecutor;
import org.eclipse.edc.sql.testfixtures.PostgresqlStoreSetupExtension;
import org.eclipse.edc.sql.testfixtures.RowLeaseUtil;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.time.Duration;

import static org.eclipse.edc.identityhub.spi.credential.request.model.HolderRequestState.ISSUED;
import static org.eclipse.edc.identityhub.spi.credential.request.model.HolderRequestState.REQUESTED;
import static org.eclipse.edc.junit.assertions.AbstractResultAssert.assertThat;

@PostgresqlIntegrationTest
@ExtendWith(PostgresqlStoreSetupExtension.class)
class SqlHolderCredentialRequestStoreTest extends HolderCredentialRequestStoreTestBase {

    private HolderCredentialRequestStoreStatements statements;
    private SqlHolderCredentialRequestStore store;
    private RowLeaseUtil leaseUtil;

    @BeforeEach
    void setup(PostgresqlStoreSetupExtension extension, QueryExecutor queryExecutor) {
        var typeManager = new JacksonTypeManager();

        statements = new PostgresDialectStatements(clock);

        store = new SqlHolderCredentialRequestStore(extension.getDataSourceRegistry(), extension.getDatasourceName(),
                extension.getTransactionContext(), typeManager::getMapper, queryExecutor, statements, RUNTIME_ID, clock);

        leaseUtil = new RowLeaseUtil(extension.getTransactionContext(), extension::getConnection, statements, statements.getHolderCredentialRequestTable(), clock);

        var schema = TestUtils.getResourceFileContentAsString("holder-credential-request-schema.sql");
        extension.runQuery(schema);
    }

    @AfterEach
    void tearDown(PostgresqlStoreSetupExtension extension) {
        extension.runQuery("DROP TABLE " + statements.getHolderCredentialRequestTable() + " CASCADE");
    }

    @Test
    void schema_shouldAddLeaseAndVersionColumnsToExistingTable(PostgresqlStoreSetupExtension extension) {
        extension.runQuery("ALTER TABLE %s DROP COLUMN leased_by, DROP COLUMN leased_at, DROP COLUMN lease_duration, DROP COLUMN version"
                .formatted(statements.getHolderCredentialRequestTable()));

        extension.runQuery(TestUtils.getResourceFileContentAsString("holder-credential-request-schema.sql"));

        var request = createRequest();
        assertThat(store.save(request)).isSucceeded();
        Assertions.assertThat(store.nextNotLeased(1)).extracting(HolderCredentialRequest::getId).containsExactly(request.getId());
        Assertions.assertThat(leaseUtil.isLeased(request.getId(), RUNTIME_ID)).isTrue();
    }

    @Test
    void save_whenChangedSinceRead_shouldFail() {
        var request = createRequest();
        store.save(request);
        var outdated = store.findById(request.getId());
        var current = store.findById(request.getId());
        current.transitionIssued("issuer-pid");
        assertThat(store.save(current)).isSucceeded();

        outdated.transitionRequested("issuer-pid");

        assertThat(store.save(outdated)).isFailed()
                .extracting(StoreFailure::getReason)
                .isEqualTo(StoreFailure.Reason.CONFLICT);
        Assertions.assertThat(store.findById(request.getId()).stateAsEnum()).isEqualTo(ISSUED);
    }

    @Test
    void save_whenSavedAgain_shouldUpdate() {
        var request = createRequest();
        store.save(request);
        var read = store.findById(request.getId());
        read.transitionRequested("issuer-pid");
        assertThat(store.save(read)).isSucceeded();

        read.transitionIssued("issuer-pid");

        assertThat(store.save(read)).isSucceeded();
        Assertions.assertThat(store.findById(request.getId()).stateAsEnum()).isEqualTo(ISSUED);
    }

    @Override
    protected SqlHolderCredentialRequestStore getStore() {
        return store;
    }

    @Override
    protected void leaseEntity(String issuanceId, String owner, Duration duration) {
        leaseUtil.leaseEntity(issuanceId, owner, duration);
    }

    @Override
    protected boolean isLeasedBy(String issuanceId, String owner) {
        return leaseUtil.isLeased(issuanceId, owner);
    }

    private HolderCredentialRequest createRequest() {
        return HolderCredentialRequest.Builder.newInstance()
                .id("request-id")
                .state(REQUESTED.code())
                .requestedCredential("test-credential-id", "TestCredential", "VC1_0_JWT")
                .participantContextId("test-participant")
                .issuerDid("did:web:testissuer")
                // older than the state timestamp of any transition in the test, which would otherwise not change it
                .stateTimestamp(clock.millis() - 1000)
                .build();
    }
}