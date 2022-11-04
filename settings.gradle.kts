rootProject.name = "identity-hub"

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

