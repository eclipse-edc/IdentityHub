package org.eclipse.dataspaceconnector.identityhub.api;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;

@OpenAPIDefinition(
        info = @Info(
                title = "Eclipse Dataspace Connector Identity Hub",
                version = "0.0.1"
        )
)
public class OpenApiCommon {
    private OpenApiCommon() {
    }
}
