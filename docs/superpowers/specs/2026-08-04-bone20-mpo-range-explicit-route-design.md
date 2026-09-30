# Bone2.0 MPO Range Explicit Route Design

## Goal

Generate compact MPO range endpoints in newly created Bone2.0 flexible-grid
SiteLink explicit routes so the frontend can draw the intended two-group
MUXPANEL64-to-FMUX32 topology without reconstructing four physical MPO fibers.

The required logical topology is:

```text
64 channels

MUXPANEL MPO1-4 -- FMUX32(slot 3) MPO1-4 -- SIG
MUXPANEL MPO5-8 -- FMUX32(slot 7) MPO1-4 -- SIG

32 channels

MUXPANEL MPO1-4 -- FMUX32 MPO1-4 -- SIG
```

One physical FMUX fan-in is already represented by one cross-connection with
four TP entries. The change collapses that four-element endpoint list to one
logical range TP; it does not merge four cross-connections.

## Scope Guard

The new representation is opt-in and applies only when all of the following
are true:

- the existing product predicate identifies the request as the Bone2.0
  `COHERENT` / `CHASSIS2.0` path (the path that selects the `ByteDance2.0`
  resource model);
- the grid is flexible (`grid=0`);
- the requested resource bandwidth is exactly 32 or 64;
- `Route` is building a SiteLink explicit route.

`SiteLinkCreator` already owns the authoritative
`isByteDance2Flex32Or64()` decision. It must explicitly enable range
aggregation on the `Route` instance. `Route` must default to the current
behavior when that opt-in is absent; it must not infer Bone2.0 from a generic
MPO name, `FMUX32`, or `hasDualFmux32` alone.

The following remain unchanged:

- all non-Bone2.0 products and resource models;
- CMUX64 and the legacy eight-port virtual `MPO` behavior;
- physical topology nodes, TP IDs, links, internal links, and XCs;
- the Bone2.0 resource JSON and YANG models;
- persisted physical `MPO1..MPO8` inventory;
- telemetry, NMS, and web code.

## Explicit Route Representation

### Path route objects

For a Bone2.0 MUXPANEL-to-FMUX32 four-fiber group, emit one logical physical
link hop instead of retaining a bare `MPO` label:

- MUXPANEL physical `MPO1..MPO4` becomes logical `MPO1-4`;
- MUXPANEL physical `MPO5..MPO8` becomes logical `MPO5-8`;
- each FMUX32 physical `MPO1..MPO4` becomes logical `MPO1-4`.

The existing equipment-pair key continues to deduplicate the four fibers into
one link per MUXPANEL-FMUX32 pair. Range selection must come from the actual
MUXPANEL endpoint numbers, not collection order. Therefore a 64-channel route
contains two logical links and a 32-channel route contains only the first
logical link.

### Cross-connections

The physical FMUX32 fan-in remains one XC whose physical endpoint contains
`MPO1`, `MPO2`, `MPO3`, and `MPO4`. In the Bone2.0 SiteLink explicit route,
replace that four-element source or destination list with one logical TP ending
in `MPO1-4`. The opposite `SIG` endpoint and direction remain unchanged.

Do not change the XC count. Do not create four `MPOn -> SIG` records, and do
not merge unrelated XCs.

Keep each `crossConnectionId` unchanged. It identifies the real physical XC
used by removal, reuse, and implementation workflows; rebuilding it from the
logical range TP would break lookup of the persisted physical XC.

### 32 versus 64

- 32 channels: one MUXPANEL group (`MPO1-4`), one FMUX32 group
  (`MPO1-4`), and no `MPO5-8` route object.
- 64 channels: two MUXPANEL groups (`MPO1-4` and `MPO5-8`) connected to the
  slot-3 and slot-7 FMUX32 cards respectively. Each FMUX32 retains its local
  logical group name `MPO1-4`.

Slot 3 and slot 7 are the existing Bone2.0 hardware contract. The aggregation
logic must still derive group membership from the real MUXPANEL-FMUX link
endpoints so reversed route construction cannot swap the range names.

## Implementation Boundary

Expected production changes are limited to:

- `SiteLinkCreator.java`: enable the new Route representation only for the
  existing Bone2.0 Flex32/Flex64 predicate and pass the selected bandwidth;
- `Route.java`: generate range-aware logical link endpoints and collapse the
  single four-entry FMUX32 XC endpoint only when the Bone2.0 opt-in is active.

Expected test changes are limited to:

- `RouteTest.java` for 32-channel, 64-channel, reversed-link, and legacy
  behavior;
- a focused `SiteLinkCreatorExternalTest.java` assertion if needed to prove
  that only the Bone2.0 Flex32/Flex64 creation path enables the option.

No platform, telemetry, web, JSON, or YANG change is part of this work.

## Compatibility and Failure Behavior

- If the Bone2.0 option is disabled, execute the existing byte-for-byte MPO
  conversion path.
- If an opted-in group is not exactly `MPO1..MPO4` or `MPO5..MPO8` on the
  MUXPANEL side, retain the existing virtual `MPO` conversion rather than
  assigning a misleading range or introducing a new creation failure.
- If an FMUX XC endpoint is not exactly one four-port `MPO1..MPO4` group,
  preserve the current XC representation; do not collapse a partial or mixed
  endpoint list.
- Existing persisted SiteLinks are not migrated. The new representation
  applies when a SiteLink explicit route is newly generated or regenerated.

## Verification

Add focused regression coverage proving:

1. A 32-channel Bone2.0 route contains one MUXPANEL `MPO1-4` logical link,
   one FMUX `MPO1-4` endpoint, and no `MPO5-8`.
2. A 64-channel Bone2.0 route contains exactly two logical equipment-pair
   links: `MPO1-4 -> MPO1-4` for slot 3 and `MPO5-8 -> MPO1-4` for slot 7.
3. Reversed physical link order produces the same equipment/range mapping.
4. Each Bone2.0 FMUX fan-in remains one XC and its four-element MPO endpoint
   becomes one `MPO1-4` TP while `SIG`, direction, APS data, sequence, and
   physical `crossConnectionId` are preserved.
5. The existing non-opted-in FMUX32 test continues to preserve its four
   physical TP entries.
6. Existing CMUX64 aggregation continues to use its legacy virtual `MPO`
   representation.
7. Non-Bone2.0 SiteLink, OCH, and tunnel route behavior is unchanged.

Run the focused `RouteTest` and `SiteLinkCreatorExternalTest` surfaces first.
Then run the proportional allocate-manager checks available in the current
generated-YANG baseline, reporting unrelated baseline failures separately.
