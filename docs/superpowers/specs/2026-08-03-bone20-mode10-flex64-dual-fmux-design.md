# Bone2.0 Mode 10 Flex64 Dual-FMUX Topology Design

## Problem

Bone2.0 flexible-grid `link-model=10` maps to `ProtectionBidir1To2`. At
64 channels, `ByteDance2OneToTwoResourceLayout` adds two `FMUX_32` cards at
slots 3 and 7, but it does not enable the existing dual-FMUX64 topology path.
The generic sequential-card builder consequently connects only one four-fiber
MPO group to the MUX panel, reverses the main and extension FMUX roles, and can
produce asymmetric A/Z endpoint topology.

The intended equipment topology is the same proven Flex64 fanout used by the
point-to-point layout, followed by the mode-10 OLP and three route legs.

## Scope

Apply only when all of the following are true:

- product is Bone2.0;
- flexible grid (`grid=0`), C band;
- protection is `ProtectionBidir1To2` (`link-model=10`);
- bandwidth is 64.

Keep 32-channel mode-10, unprotected Flex64, protected 1:1 Flex64, fixed-grid,
other products, YANG, persistence schemas, telemetry, and web unchanged.

## Required Topology

At both A and Z endpoints:

1. MUXPANEL `MPO1..4` connects to slot-3 FMUX `MPO1..4`.
2. MUXPANEL `MPO5..8` connects to slot-7 FMUX `MPO1..4`.
3. Slot-7 FMUX `SIG` connects to slot-3 FMUX `COM1`.
4. Slot 3 is the main/protection-capable FMUX and owns the required internal
   self-link and APS cross-connection.
5. Slot-3 FMUX `SIGA` connects to OLP3_3 `1SIG`.
6. OLP3_3 routes `1A` to the main TILA, `1B` to the slave TILA, and `1C` to the
   third TILA.
7. Reversed endpoint construction produces the same physical relationships
   with link direction reversed, not different slot roles.

## Implementation

Reuse the existing dual-FMUX64 link builder rather than duplicating the fanout
logic.

- `ByteDance2OneToTwoResourceLayout` reports dual-FMUX64 topology only when its
  bandwidth is 64. It also supplies the slot-3 FMUX self-link, APS-XC, and
  `SIGA` line-port behavior required by the shared builder.
- `SiteNodeService.createDualFmux64Links` supports an optional OLP3_3 card. When
  present, it connects the main FMUX to OLP `1SIG`, connects OLP `1A` to the
  main TILA, and returns the OLP as the protected card so the existing slave
  and third-link creation continues to consume `1B` and `1C`.
- Without OLP3_3, the current point-to-point Flex64 behavior is unchanged.

No card selection may depend on list order. Slot 3 and slot 7 remain the
explicit hardware contract.

## Failure Handling

The shared dual-FMUX builder continues to fail explicitly when a required card,
slot, port relationship, or expected link count is absent. It must not fall
back to the generic sequential-card path for a 64-channel mode-10 request.

## Tests

Add regression coverage that fails against the current implementation and
proves:

- 64-channel mode-10 enables dual-FMUX64 behavior while 32-channel mode-10 does
  not;
- both MUXPANEL-to-FMUX groups contain four links;
- slot 7 connects to slot 3 through `SIG -> COM1`;
- slot 3, not slot 7, owns the protection XC and self-link;
- slot-3 `SIGA` connects to OLP `1SIG`;
- OLP `1A`, `1B`, and `1C` still feed main, slave, and third TILA routes;
- A-to-Z and reversed Z-to-A construction produce the same slot/port contract;
- existing unprotected and 1:1 Flex64 tests remain green.

## Deployment Boundary

The code change affects newly designed/recreated SiteLinks. It does not mutate
an already persisted incorrect SiteLink or its physical-node documents.
