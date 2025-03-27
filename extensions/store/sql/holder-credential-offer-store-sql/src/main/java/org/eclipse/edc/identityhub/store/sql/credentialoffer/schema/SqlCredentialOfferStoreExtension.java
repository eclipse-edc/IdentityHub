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

package org.eclipse.edc.identityhub.store.sql.credentialoffer.schema;

import org.eclipse.edc.identityhub.spi.verifiablecredentials.store.CredentialOfferStore;
import org.eclipse.edc.identityhub.store.sql.credentialoffer.schema.schema.postgres.PostgresDialectStatements;
import org.eclipse.edc.runtime.metamodel.annotation.Extension;
import org.eclipse.edc.runtime.metamodel.annotation.Inject;
import org.eclipse.edc.runtime.metamodel.annotation.Provider;
import org.eclipse.edc.runtime.metamodel.annotation.Setting;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.eclipse.edc.spi.types.TypeManager;
import org.eclipse.edc.sql.QueryExecutor;
import org.eclipse.edc.sql.bootstrapper.SqlSchemaBootstrapper;
import org.eclipse.edc.transaction.datasource.spi.DataSourceRegistry;
import org.eclipse.edc.transaction.spi.TransactionContext;

import java.time.Clock;

@Extension(value = SqlCredentialOfferStoreExtension.NAME)
public class SqlCredentialOfferStoreExtension implements ServiceExtension {
    public static final String NAME = "Issuance Process SQL Store Extension";

    @Setting(description = "The datasource to be used", defaultValue = DataSourceRegistry.DEFAULT_DATASOURCE, key = "edc.sql.store.credentialrequest.datasource")
    private String dataSourceName;

    @Inject
    private DataSourceRegistry dataSourceRegistry;
    @Inject
    private TransactionContext transactionContext;
    @Inject
    private TypeManager typemanager;
    @Inject
    private QueryExecutor queryExecutor;
    @Inject(required = false)
    private CredentialOfferStoreStatements statements;
    @Inject
    private SqlSchemaBootstrapper sqlSchemaBootstrapper;

    @Inject
    private Clock clock;


    @Override
    public void initialize(ServiceExtensionContext context) {
        sqlSchemaBootstrapper.addStatementFromResource(dataSourceName, "credential-offer-schema.sql");
    }

    @Provider
    public CredentialOfferStore createSqlStore(ServiceExtensionContext context) {
        return new SqlCredentialOfferStore(dataSourceRegistry, dataSourceName, transactionContext, typemanager.getMapper(),
                queryExecutor, getStatementImpl(), context.getRuntimeId(), clock);
    }

    private CredentialOfferStoreStatements getStatementImpl() {
        return statements != null ? statements : new PostgresDialectStatements();
    }

}
