/*
 *  Copyright (c) 2020 - 2023 Amadeus
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Amadeus - initial API and implementation
 *
 */

package org.eclipse.edc.identityhub.store.sql;

import org.eclipse.edc.identityhub.store.spi.IdentityHubStore;
import org.eclipse.edc.identityhub.store.sql.schema.BaseSqlIdentityHubStatements;
import org.eclipse.edc.identityhub.store.sql.schema.IdentityHubStatements;
import org.eclipse.edc.runtime.metamodel.annotation.Extension;
import org.eclipse.edc.runtime.metamodel.annotation.Inject;
import org.eclipse.edc.runtime.metamodel.annotation.Provider;
import org.eclipse.edc.runtime.metamodel.annotation.Setting;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.eclipse.edc.transaction.datasource.spi.DataSourceRegistry;
import org.eclipse.edc.transaction.spi.TransactionContext;

import java.util.Objects;

/**
 * Extension that provides a {@link IdentityHubStore} with SQL as backend storage.
 */
@Extension(value = SqlIdentityHubStoreExtension.NAME)
public class SqlIdentityHubStoreExtension implements ServiceExtension {

    public static final String NAME = "SQL Identity Hub Store";

    @Setting(value = "Name of the datasource in which store items are persisted")
    private static final String DATASOURCE_NAME_SETTING = "edc.datasource.identityhub.name";
    private static final String DEFAULT_DATASOURCE_NAME = "identityhub";

    @Inject(required = false)
    private IdentityHubStatements statements;
    @Inject
    private DataSourceRegistry dataSourceRegistry;
    @Inject
    private TransactionContext trxContext;

    @Override
    public String name() {
        return NAME;
    }

    @Provider
    public IdentityHubStore identityHubStore(ServiceExtensionContext context) {
        var s = Objects.requireNonNullElse(statements, new BaseSqlIdentityHubStatements());
        var dataSource = context.getSetting(DATASOURCE_NAME_SETTING, DEFAULT_DATASOURCE_NAME);
        return new SqlIdentityHubStore(dataSourceRegistry, dataSource, trxContext, s, context.getTypeManager().getMapper());
    }
}
