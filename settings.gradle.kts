rootProject.name = "identity-hub"

include(":spi:identity-hub-spi")
include(":spi:identity-hub-store-spi")
include(":extensions:identity-hub")
include(":identity-hub-core:identity-hub-client")
include(":identity-hub-core:identity-hub-model")
include(":extensions:identity-hub-verifier")
include(":extensions:store:sql:identity-hub-store-sql")
include(":client-cli")
include(":launcher")
include(":system-tests")
