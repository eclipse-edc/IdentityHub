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

package org.eclipse.edc.identityhub.store.sql.keypair;

import org.eclipse.edc.identityhub.spi.store.KeyPairResourceStore;
import org.eclipse.edc.identityhub.store.sql.keypair.schema.postgres.PostgresDialectStatements;
import org.eclipse.edc.runtime.metamodel.annotation.Extension;
import org.eclipse.edc.runtime.metamodel.annotation.Inject;
import org.eclipse.edc.runtime.metamodel.annotation.Provider;
import org.eclipse.edc.runtime.metamodel.annotation.Setting;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.eclipse.edc.spi.types.TypeManager;
import org.eclipse.edc.sql.QueryExecutor;
import org.eclipse.edc.sql.bootstrapper.SqlSchemaBootstrapper;
import org.eclipse.edc.sql.configuration.DataSourceName;
import org.eclipse.edc.transaction.datasource.spi.DataSourceRegistry;
import org.eclipse.edc.transaction.spi.TransactionContext;

import static org.eclipse.edc.identityhub.store.sql.keypair.SqlKeyPairResourceStoreExtension.NAME;

@Extension(NAME)
public class SqlKeyPairResourceStoreExtension implements ServiceExtension {
    public static final String NAME = "KeyPair Resource SQL Store Extension";

    @Deprecated(since = "0.8.1")
    @Setting(value = "Datasource name for the KeyPairResource database", defaultValue = DataSourceRegistry.DEFAULT_DATASOURCE)
    public static final String DATASOURCE_SETTING_NAME = "edc.datasource.keypair.name";
    @Setting(value = "The datasource to be used", defaultValue = DataSourceRegistry.DEFAULT_DATASOURCE)
    public static final String DATASOURCE_NAME = "edc.sql.store.keypair.datasource";

    @Inject
    private DataSourceRegistry dataSourceRegistry;

    @Inject
    private TransactionContext transactionContext;

    @Inject
    private TypeManager typemanager;

    @Inject
    private QueryExecutor queryExecutor;

    @Inject(required = false)
    private KeyPairResourceStoreStatements statements;

    @Inject
    private SqlSchemaBootstrapper sqlSchemaBootstrapper;

    @Override
    public String name() {
        return NAME;
    }

    @Override
    public void initialize(ServiceExtensionContext context) {
        sqlSchemaBootstrapper.addStatementFromResource(getDataSourceName(context), "keypairs-schema.sql");
    }

    @Provider
    public KeyPairResourceStore createSqlStore(ServiceExtensionContext context) {
        return new SqlKeyPairResourceStore(dataSourceRegistry, getDataSourceName(context), transactionContext, typemanager.getMapper(),
                queryExecutor, getStatementImpl());
    }

    private KeyPairResourceStoreStatements getStatementImpl() {
        return statements != null ? statements : new PostgresDialectStatements();
    }

    private String getDataSourceName(ServiceExtensionContext context) {
        return DataSourceName.getDataSourceName(DATASOURCE_NAME, DATASOURCE_SETTING_NAME, context.getConfig(), context.getMonitor());
    }
}
