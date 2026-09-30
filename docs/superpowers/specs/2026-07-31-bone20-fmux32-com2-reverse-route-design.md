# Bone2.0 FMUX32 COM2 And Reverse Route Design

## Problem

The Flex32 hardware model uses the fixed chain `MPO -> SIG -> COM2` and APS
fanout `COM2 -> SIGA,SIGB`. Current allocation selects COM1 instead. SiteLink
ERO construction also orients same-equipment links by equipment ID only, so the
Z-end reverse path is serialized as `SIGA -> SIG -> COM1 -> MPO` instead of
following the declared XCs.

MPO aggregation additionally truncates the virtual XC ID after `MPO`, dropping
the SIG endpoint.

## Design

- Select the FMUX32 `SIG -> COM2` self-link for Bone2.0 Flex32.
- Rewrite only the FMUX APS XC to `COM2 -> SIGA,SIGB`.
- When both endpoints of a candidate link belong to the same equipment and the
  current TP does not identify one directly, use route XCs to choose the link
  endpoint reachable from the current TP.
- Pass the relevant main/slave/third XC list into ERO construction so route
  orientation is scoped to the correct leg.
- Rebuild an aggregated XC ID from the collapsed source and destination TP
  lists, preserving both virtual `MPO` and `SIG` endpoints.

## Expected Routes

- A end: `MPO -> SIG -> COM2 -> SIGA`.
- D end: `SIGA -> COM2 -> SIG-COM2 link -> SIG -> MPO`.
- Protection remains `SIGB -> TILA slot 5 -> direct span -> TILA slot 5 -> SIGB`.

## Scope

- Controller only, branch `bytedance`.
- No frontend or platform changes.
- Preserve existing equipment, TP, link and protection identifiers.
- No migration of historical SiteLinks; recomputed/new SiteLinks receive the
  corrected route.

## Tests

- Layout selects COM2 and rejects COM1 for the Flex32 self-link.
- APS adaptation produces `COM2 -> SIGA,SIGB`.
- Same-card reverse traversal uses XC reachability to orient SIG-COM2.
- Aggregated FMUX XC ID contains both virtual MPO and SIG endpoints.
