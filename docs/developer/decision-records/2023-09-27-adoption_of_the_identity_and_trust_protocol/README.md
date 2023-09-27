# Identity Hub - Adoption of the Identity And Trust Protocols

## Decision

The Identity Hub component will adopt the Identity And Trust Protocols (
cf. [this decision-record](https://github.com/eclipse-edc/docs/tree/e7730f432305775542503e4ecb61aa7e829bea30/developer/decision-records/2023-09-06-identity-trust)).
To that end, the current content of the repository will be preserved in a branch named `identity_dwn` (to indicate the
partial implementation of the DWN Spec).

All further developments will happen on the `main` branch.

## Rationale

The adoption of the Identity and Trust Protocols spec may bring some substantial changes, for example it will introduce
a series of new APIs.

The entire work package is too large to be done in one feature branch, so we will do the adoption iteratively.

To avoid potential conflicts with existing implementations (model classes, interfaces, etc.) the current state of the
IdentityHub will be preserved in a separate branch.

It's generally good practice to have the `main` branch reflect the most recent state of development, but there are
other, more technical advantages as well:

- some GitHub actions only run on the `main` branch
- new workflows are only detected once they are on the `main` branch
- cloning the repo by default clones the `main` branch
- creating PRs targets the `main` branch - that would constitute a significant potential of PRs that target the wrong
  head branch

## Approach

- create a new branch `identity_dwn`
- add architecture documents
- add GitHub issues for the upcoming developments