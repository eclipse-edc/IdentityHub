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


import org.eclipse.edc.issuerservice.spi.issuance.model.AttestationDefinition;
import org.eclipse.edc.spi.persistence.EdcPersistenceException;
import org.eclipse.edc.sql.QueryExecutor;
import org.eclipse.edc.transaction.datasource.spi.DataSourceRegistry;
import org.eclipse.edc.transaction.spi.TransactionContext;
import org.eclipse.edc.validator.spi.ValidationResult;
import org.eclipse.edc.validator.spi.Validator;

import java.sql.SQLException;
import javax.sql.DataSource;

import static java.lang.String.format;
import static org.eclipse.edc.issuerservice.issuance.database.DatabaseAttestationSource.DATASOURCE_NAME;
import static org.eclipse.edc.issuerservice.issuance.database.DatabaseAttestationSource.DEFAULT_ID_COLUMN;
import static org.eclipse.edc.issuerservice.issuance.database.DatabaseAttestationSource.ID_COLUMN;
import static org.eclipse.edc.issuerservice.issuance.database.DatabaseAttestationSource.TABLE_NAME;
import static org.eclipse.edc.validator.spi.ValidationResult.failure;
import static org.eclipse.edc.validator.spi.ValidationResult.success;
import static org.eclipse.edc.validator.spi.Violation.violation;

/**
 * Validates database attestation definitions. In addition to structural checks of the configuration, it verifies that
 * the configured datasource is registered on the runtime and that the configured table and ID column actually exist,
 * so that misconfigurations fail at attestation creation time rather than during credential issuance.
 */
public class DatabaseAttestationSourceValidator implements Validator<AttestationDefinition> {
    private static final String ATTESTATION_TYPE = "database";
    private static final String PROBE_QUERY_TEMPLATE = "SELECT %s FROM %s WHERE 1 = 0";

    private final DataSourceRegistry dataSourceRegistry;
    private final TransactionContext transactionContext;
    private final QueryExecutor queryExecutor;

    public DatabaseAttestationSourceValidator(DataSourceRegistry dataSourceRegistry, TransactionContext transactionContext, QueryExecutor queryExecutor) {
        this.dataSourceRegistry = dataSourceRegistry;
        this.transactionContext = transactionContext;
        this.queryExecutor = queryExecutor;
    }

    @Override
    public ValidationResult validate(AttestationDefinition definition) {
        if (!ATTESTATION_TYPE.equals(definition.getAttestationType())) {
            return failure(violation("Expecting attestation type: " + ATTESTATION_TYPE, ATTESTATION_TYPE));
        }
        var config = definition.getConfiguration();
        if (!(config.get(DATASOURCE_NAME) instanceof String dataSourceName) || dataSourceName.isBlank()) {
            return failure(violation(format("No %s specified", DATASOURCE_NAME), DATASOURCE_NAME));
        }
        if (!(config.get(TABLE_NAME) instanceof String tableName) || tableName.isBlank()) {
            return failure(violation(format("No %s specified", TABLE_NAME), TABLE_NAME));
        }
        var idColumn = config.get(ID_COLUMN) instanceof String col && !col.isBlank() ? col : DEFAULT_ID_COLUMN;

        var dataSource = dataSourceRegistry.resolve(dataSourceName);
        if (dataSource == null) {
            return failure(violation(format("DataSource '%s' not found. It must be configured on the runtime.", dataSourceName), DATASOURCE_NAME));
        }

        return verifySchema(dataSource, tableName, idColumn);
    }

    private ValidationResult verifySchema(DataSource dataSource, String tableName, String idColumn) {
        try {
            return transactionContext.execute(() -> {
                try (var connection = dataSource.getConnection()) {
                    queryExecutor.single(connection, false, resultSet -> 0, PROBE_QUERY_TEMPLATE.formatted(idColumn, tableName));
                    return success();
                } catch (SQLException | EdcPersistenceException e) {
                    return failure(violation(format("Table '%s' with column '%s' could not be queried on the configured datasource: %s",
                            tableName, idColumn, e.getMessage()), TABLE_NAME));
                }
            });
        } catch (RuntimeException e) {
            return failure(violation(format("Could not verify datasource: %s", e.getMessage()), DATASOURCE_NAME));
        }
    }
}
