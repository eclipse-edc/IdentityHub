package org.eclipse.edc.identityhub.store.sql;

import org.eclipse.edc.identityhub.store.spi.IdentityHubRecord;
import org.eclipse.edc.identityhub.store.sql.schema.BaseSqlIdentityHubStatements;
import org.eclipse.edc.spi.types.TypeManager;
import org.eclipse.edc.transaction.datasource.spi.DefaultDataSourceRegistry;
import org.eclipse.edc.transaction.spi.NoopTransactionContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.io.IOException;
import java.sql.ResultSet;
import java.sql.SQLException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

public class SqlIdentityHubStoreTest {

    @Mock
    private ResultSet resultSet;

    private SqlIdentityHubStore sqlIdentityHubStore;

    @BeforeEach
    void setUp() throws IOException {
        var statements = new BaseSqlIdentityHubStatements();
        var typeManager = new TypeManager();
        var dataSourceRegistry = new DefaultDataSourceRegistry();
        var transactionContext = new NoopTransactionContext();

        MockitoAnnotations.openMocks(this);

        sqlIdentityHubStore = new SqlIdentityHubStore(dataSourceRegistry, "dataSourceName", transactionContext, statements, typeManager.getMapper());
    }


    @Test
    public void testParse() throws SQLException {
        when(resultSet.getString("id")).thenReturn("123");
        when(resultSet.getString("payloadFormat")).thenReturn("json");
        when(resultSet.getString("payload")).thenReturn("{\"key\": \"value\"}");
        when(resultSet.getLong("created_at")).thenReturn(1625385600000L);

        IdentityHubRecord result = sqlIdentityHubStore.parse(resultSet);

        assertEquals("123", result.getId());
        assertEquals("json", result.getPayloadFormat());
        assertEquals("{\"key\": \"value\"}", new String(result.getPayload()));
        assertEquals(1625385600000L, result.getCreatedAt());
    }
}
