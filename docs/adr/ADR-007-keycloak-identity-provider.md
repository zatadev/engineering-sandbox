# ADR-007: Keycloak as OAuth2/OIDC Identity Provider

## Status: Accepted

## Context

Phase 8 introduces authentication and authorization across all services. A decision
is needed on the identity provider (IdP) to use for OAuth2/OIDC token issuance.

The sandbox targets enterprise environments where identity management is handled
by a dedicated IdP rather than custom auth logic. The IdP must support OAuth2,
OpenID Connect, JWT tokens, and role-based access control (RBAC).

Alternatives considered:
- Keycloak — open-source, self-hosted IdP with full OAuth2/OIDC support
- Auth0 — managed IdP, SaaS model
- Spring Authorization Server — lightweight, Spring-native OAuth2 server
- Okta — enterprise managed IdP

## Decision

Use **Keycloak** as the self-hosted OAuth2/OIDC identity provider.

Keycloak will run as a Docker container in the local environment. It will be configured
with a dedicated realm, clients for each service, and roles for RBAC. Spring Boot services
will be configured as OAuth2 resource servers, validating JWT tokens issued by Keycloak.

## Consequences

**Positive:**
- Keycloak is the dominant self-hosted IdP in European enterprise and banking environments —
  direct relevance to the target job market
- Full OAuth2/OIDC support: authorization code flow, client credentials, token introspection,
  refresh tokens, PKCE
- RBAC configuration via realm roles and client roles maps directly to real-world patterns
- Self-hosted: no SaaS dependency, no cost, full control over realm configuration
- Spring Security has first-class support for Keycloak as a resource server
- Demonstrates end-to-end security architecture — a strong differentiator in senior interviews

**Negative:**
- Keycloak has a steep initial configuration curve (realms, clients, scopes, mappers)
- Resource-intensive to run locally alongside other Docker containers
- Upgrading Keycloak versions can introduce breaking changes in realm configuration

**Neutral:**
- Auth0 and Okta are valid managed alternatives in SaaS or startup contexts —
  Keycloak is chosen for its self-hosted model and enterprise relevance
- Token propagation between services (service-to-service auth) will use client credentials flow
- HashiCorp Vault will handle secret injection (client secrets, keystore passwords) — see Phase 8
