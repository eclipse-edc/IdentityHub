rootProject.name = "identity-hub"

val edcVersion: String by settings
val bouncyCastleVersion: String by settings
val picocliVersion: String by settings
val edcGroup: String by settings
val edcGradlePluginsVersion: String by settings

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
            from("${edcGroup}:edc-versions:${edcGradlePluginsVersion}")
            version("picocli", picocliVersion)
            version("bouncycastle", bouncyCastleVersion)

            library("picocli-core", "info.picocli", "picocli").versionRef("picocli")
            library("picocli-codegen", "info.picocli", "picocli-codegen").versionRef("picocli")
            library("bouncycastle-bcpkix-jdk15on", "org.bouncycastle", "bcpkix-jdk15on").versionRef("bouncycastle")


        }
        create("edc") {
            version("edc", edcVersion)
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

        }
    }
}

include(":spi:identity-hub-spi")
include(":spi:identity-hub-store-spi")
include(":spi:identity-hub-client-spi")
include(":core:identity-hub")
include(":core:identity-hub-client")
include(":extensions:identity-hub-verifier")
include(":extensions:store:sql:identity-hub-store-sql")
include(":extensions:identity-hub-api")
include(":client-cli")
include(":launcher")
include(":system-tests")

