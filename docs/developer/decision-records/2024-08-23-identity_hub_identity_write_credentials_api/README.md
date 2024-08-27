# Identity Hub - Identity Write Credentials API

## Decision

Write endpoints (create/update) will be added to the Identity Hub identity API to
give users a way to add VCs that weren't issued using DCP Issuance, but using some other means, or enable
users to populate the VC storage with existing VCs after moving to a new IH instance or similar.

## Rationale

VCs might not always be issued through DCP Issuance, but instead through some other means (e.g. download json file
containing the VC from a website...). In such cases, users need a way to add these VCs to the Identity Hub. This is why
we introduce write/update endpoints to the Identity Hub identity API.

> **_NOTE:_**  It is worth emphasizing that these new endpoints must not be used as issuance APIs, and thus should
> not exposed publicly over the internet. The same warning applies for all other endpoints under the `identity` API
> context.