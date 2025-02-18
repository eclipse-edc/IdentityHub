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

package org.eclipse.edc.issuerservice.store.sql.credentialdefinition;

import org.eclipse.edc.issuerservice.spi.issuance.credentialdefinition.store.CredentialDefinitionStore;
import org.eclipse.edc.issuerservice.store.sql.credentialdefinition.schema.postgres.PostgresDialectStatements;
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

import static org.eclipse.edc.issuerservice.store.sql.credentialdefinition.SqlCredentialDefinitionStoreExtension.NAME;

@Extension(value = NAME)
public class SqlCredentialDefinitionStoreExtension implements ServiceExtension {
    public static final String NAME = "IssuerService Credential definition SQL Store Extension";

    @Setting(description = "The datasource to be used", defaultValue = DataSourceRegistry.DEFAULT_DATASOURCE, key = "edc.sql.store.credentialdefinitions.datasource")
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
    private CredentialDefinitionStoreStatements statements;
    @Inject
    private SqlSchemaBootstrapper sqlSchemaBootstrapper;

    @Inject
    private Clock clock;

    @Override
    public void initialize(ServiceExtensionContext context) {
        sqlSchemaBootstrapper.addStatementFromResource(dataSourceName, "credential-definition-schema.sql");
    }

    @Provider
    public CredentialDefinitionStore createSqlStore() {
        return new SqlCredentialDefinitionStore(dataSourceRegistry, dataSourceName, transactionContext, typemanager.getMapper(),
                queryExecutor, getStatementImpl(), clock);
    }

    private CredentialDefinitionStoreStatements getStatementImpl() {
        return statements != null ? statements : new PostgresDialectStatements();
    }

}
