rootProject.name = "identity-hub"

// this is needed to have access to snapshot builds of plugins
pluginManagement {
    repositories {
        maven {
            url = uri("https://oss.sonatype.org/content/repositories/snapshots/")
        }
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {

    repositories {
        maven {
            url = uri("https://oss.sonatype.org/content/repositories/snapshots/")
        }
        mavenCentral()
        mavenLocal()
    }
    versionCatalogs {
        create("libs") {
            from("org.eclipse.edc:edc-versions:0.0.1-SNAPSHOT")
            version("picocli", "4.6.3")

            library("picocli-core", "info.picocli", "picocli").versionRef("picocli")
            library("picocli-codegen", "info.picocli", "picocli-codegen").versionRef("picocli")
            library("swagger-jaxrs", "io.swagger.core.v3", "swagger-jaxrs2-jakarta").version("2.1.13")
        }
        create("edc") {
            version("edc", "0.0.1-SNAPSHOT")
            library("spi-core", "org.eclipse.edc", "core-spi").versionRef("edc")
            library("spi-transaction", "org.eclipse.edc", "transaction-spi").versionRef("edc")
            library("spi-transaction-datasource", "org.eclipse.edc", "transaction-datasource-spi").versionRef("edc")
            library("spi-identity-did", "org.eclipse.edc", "identity-did-spi").versionRef("edc")
            library("core-connector", "org.eclipse.edc", "connector-core").versionRef("edc")
            library("core-sql", "org.eclipse.edc", "sql-core").versionRef("edc")
            library("core-identity-did", "org.eclipse.edc", "identity-did-core").versionRef("edc")
            library("core-junit", "org.eclipse.edc", "junit").versionRef("edc")
            library("ext-identity-did-crypto", "org.eclipse.edc", "identity-did-crypto").versionRef("edc")
            library("ext-identity-did-core", "org.eclipse.edc", "identity-did-core").versionRef("edc")
            library("ext-identity-did-web", "org.eclipse.edc", "identity-did-web").versionRef("edc")
            library("ext-http", "org.eclipse.edc", "http").versionRef("edc")
            library("ext-observability", "org.eclipse.edc", "api-observability").versionRef("edc")
            library("ext-azure-cosmos-core", "org.eclipse.edc", "azure-cosmos-core").versionRef("edc")
            library("ext-azure-test", "org.eclipse.edc", "azure-test").versionRef("edc")
        }
    }
}

include(":spi:identity-hub-spi")
include(":spi:identity-hub-store-spi")
include(":spi:identity-hub-client-spi")
include(":core:identity-hub")
include(":core:identity-hub-client")
include(":core:identity-hub-verifier")


include(":extensions:store:sql:identity-hub-store-sql")
include(":extensions:store:cosmos:identity-hub-store-cosmos")
include(":extensions:identity-hub-api")
include(":extensions:identity-hub-verifier-jwt")
include(":extensions:credentials:identity-hub-credentials-jwt")


include(":client-cli")
include(":launcher")
include(":system-tests")
include("extensions:identity-hub-verifier-jwt")
