# BONE2.0 SiteLink APS Route Selection Design

## Scope

Fix only BONE2.0 protected SiteLink explicit-route creation in
`allocate/manager`. Do not change Tunnel/OCH third-leg binding, topology cache,
DB change monitoring, status/alarm handling, YANG models, telemetry, or web.

The fix must preserve single-APS SiteLink behavior used by commercial C and
legacy protection models.

## Root Cause

`Route.getExplictRoute` currently chooses the first APS cross-connection on the
A-end node. A BONE2.0 Flex endpoint can contain both an internal FMUX APS and an
OLP APS. Cross-connection ordering is not a topology contract, so the FMUX APS
can be selected and then cannot provide the SiteLink third-leg TP.

Filtering candidates only by whether they contain three source or destination
TPs hides the immediate exception but still treats port count and collection
order as topology semantics.

## Selection Contract

1. Collect APS cross-connections on the A-end node.
2. If there is no APS, preserve unprotected behavior.
3. If there is exactly one APS, preserve the existing protected SiteLink path.
4. When a slave and third route exist and multiple APS candidates exist, match
   each APS branch TP against the endpoints of the actual slave and third
   physical links.
5. Select an APS only when it uniquely supplies one slave TP and one different
   third TP.
6. Use those matched TPs as the slave and third explicit-route starting TPs.
7. If zero or multiple candidates satisfy the relationship, fail explicitly
   with candidate context. Never fall back to the first candidate.

The algorithm must not depend on card type, equipment slot, literal port names,
source-versus-destination orientation, or cross-connection list order.

## Compatibility

- A SiteLink with one APS continues to use the existing source/destination
  ordering behavior.
- A two-route SiteLink continues to use the existing APS selection behavior.
- The new semantic matching path applies only when multiple A-end APS
  candidates coexist with both slave and third routes.
- No public API, persistence schema, YANG, Kafka, RESTCONF, or UI contract
  changes are introduced.

## Tests

Add focused `RouteTest` cases proving:

- FMUX first and OLP second selects the OLP by route membership.
- OLP first and FMUX second produces the same result.
- Reversed APS source/destination orientation works.
- Ambiguous candidates are rejected instead of silently selecting the first.
- A single legacy APS retains the existing behavior.

The production change that makes the mixed-APS tests fail is reintroducing
collection-order selection or fixed source/destination indexes.
