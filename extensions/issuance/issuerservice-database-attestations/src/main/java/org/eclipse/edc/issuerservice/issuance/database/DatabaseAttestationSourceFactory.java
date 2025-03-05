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

package org.eclipse.edc.issuerservice.issuance.database;


import com.fasterxml.jackson.databind.ObjectMapper;
import org.eclipse.edc.issuerservice.spi.issuance.attestation.AttestationSource;
import org.eclipse.edc.issuerservice.spi.issuance.attestation.AttestationSourceFactory;
import org.eclipse.edc.issuerservice.spi.issuance.model.AttestationDefinition;
import org.eclipse.edc.sql.QueryExecutor;
import org.eclipse.edc.transaction.datasource.spi.DataSourceRegistry;
import org.eclipse.edc.transaction.spi.TransactionContext;

import static org.eclipse.edc.issuerservice.issuance.database.DatabaseAttestationSource.DATASOURCE_NAME;
import static org.eclipse.edc.issuerservice.issuance.database.DatabaseAttestationSource.ID_COLUMN;
import static org.eclipse.edc.issuerservice.issuance.database.DatabaseAttestationSource.REQUIRED;
import static org.eclipse.edc.issuerservice.issuance.database.DatabaseAttestationSource.TABLE_NAME;

public class DatabaseAttestationSourceFactory implements AttestationSourceFactory {

    private final TransactionContext transactionContext;
    private final QueryExecutor queryExecutor;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final DataSourceRegistry dataSourceRegistry;

    public DatabaseAttestationSourceFactory(TransactionContext transactionContext, QueryExecutor queryExecutor, DataSourceRegistry dataSourceRegistry) {
        this.transactionContext = transactionContext;
        this.queryExecutor = queryExecutor;
        this.dataSourceRegistry = dataSourceRegistry;
    }

    @Override
    public AttestationSource createSource(AttestationDefinition definition) {
        var configuration = definition.getConfiguration();
        var required = (Boolean) configuration.getOrDefault(REQUIRED, false);
        var dataSourceName = (String) configuration.get(DATASOURCE_NAME);
        var tableName = (String) configuration.get(TABLE_NAME);
        var idColumn = (String) configuration.getOrDefault(ID_COLUMN, "holder_id");

        return new DatabaseAttestationSource(dataSourceName, required, objectMapper, tableName, dataSourceRegistry, queryExecutor, transactionContext, idColumn);
    }
}
