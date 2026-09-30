# Restore TILA Class-Mode Contract Design

## Goal

Restore the BONE2.0 TILA equipment-level `class-mode` contract in
`controller/bytedance` after platform `bytedance` restored the corresponding
YANG leaf and Mongo conversion.

## Design

Reverse only the production and regression-test deletions introduced by
`6aaeac337` (`fix: remove obsolete TILA class-mode contract`) on top of the
current local `controller/bytedance` head. Endpoint TILA equipment emits
`OTA_WEST`; transit TILA equipment emits `ILA`; non-TILA and non-BONE2.0
equipment remains unchanged.

The restoration stays inside `allocate/designer`. It does not change physical
ports, cross-connections, slot selection, routing, frequency selection, web,
NMS, telemetry, or persistence schemas. The later `c8d2f4d16` Bone2.0
frequency-model change remains intact.

## Verification

- First restore the class-mode layout assertions and confirm they fail while
  the production methods are absent.
- Restore the production methods and equipment builder call.
- Run the focused `SiteResourceLayoutFactoryTest` suite and the affected
  allocate designer compilation/tests.
- Confirm the final diff is the semantic inverse of `6aaeac337` plus these
  design/plan records, with unrelated untracked files untouched.
