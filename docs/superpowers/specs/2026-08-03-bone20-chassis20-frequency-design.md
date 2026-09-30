# Bone2.0 CHASSIS2.0 Frequency Range Design

## Problem

Bone2.0 `CHASSIS2.0` SiteLinks currently reuse the global `ByteDance` frequency
model. For a fixed 150 GHz grid, `FrequencyAvailable` therefore starts from
`191250000` and creates 32 intervals whose centers are all 75 GHz too low.

The hardware frequency plan requires 32 channels with centers
`191400000 + n * 150000`, where `n` is 0 through 31. The correct occupied range
is therefore `191325000` through `196125000`.

## Selected Approach

Introduce an explicit `Chassis20` Java model whose external name is
`Chassis2.0`. Give that model its own C-band lower and upper constants and select
it whenever allocation handles the normalized `COHERENT` / `CHASSIS2.0`
product.

This keeps the existing `ByteDance` and C+L ranges unchanged. A global change
to the ByteDance lower bound is rejected because it would alter legacy products.

## Components and Data Flow

1. `NeYangModel` defines `Chassis20` with the same mux-port naming contract as
   ByteDance.
2. `Constant` defines the Chassis2.0 C-band range as `191325000` to
   `196125000`.
3. `SiteLinkCreator` resolves the per-SiteLink frequency model after parsing
   vendor and product type. Bone2.0 uses `Chassis20`; every other product keeps
   the configured global model.
4. New Bone2.0 SiteLinks persist `yang-model=Chassis20` and initialize their
   `available` list through the Chassis2.0 case.
5. `FrequencyAvailable` also recognizes an existing SiteLink whose product type
   is `CHASSIS2.0`, so subsequent frequency calculations use the correct model
   even if the older SiteLink still stores `yang-model=ByteDance`.
6. For grid 150, the generator produces exactly 32 adjacent intervals. The
   first is `191325000-191475000`; the last is
   `195975000-196125000`.

## Compatibility and Error Handling

- `ByteDance`, `Tencent`, and `ChinaTelecom` frequency ranges remain unchanged.
- Chassis2.0 remains C-band only; existing validation continues to reject L and
  C+L input for Bone2.0.
- The Chassis2.0 model uses the existing ByteDance mux-port format so downstream
  naming code does not reject the new enum.
- Existing persisted data is not rewritten automatically. This change corrects
  creation and model-aware calculations; any production repair remains a
  separate explicitly authorized operation.

## Tests

- Add a platform regression test for Chassis2.0 grid 150 that asserts:
  - 32 intervals;
  - first and last interval boundaries;
  - first and last center frequencies;
  - every adjacent interval advances by 150000.
- Add compatibility assertions proving the ByteDance grid-150 lower bound stays
  `191250000`.
- Add controller tests proving `COHERENT/CHASSIS2.0` selects `Chassis20` and a
  legacy product keeps the configured model.

## Non-Goals

- Do not change `order-id` data.
- Do not edit resource JSON, YANG, PPT, web, or NMS code.
- Do not change the L-band range or legacy ByteDance C/C+L behavior.
- Do not mutate MongoDB or automatically repair deployed SiteLinks.
