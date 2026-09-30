# Bone2.0 Flex64 OMSP 1:3 Protection Peer Design

## Problem

Bone2.0 flexible-grid 64-channel OMSP 1:3 uses
`ProtectionBidir1To2` and stores three routes in one SiteLink: main, slave, and
third. Each endpoint site contains a full main NE and a dedicated protection
peer NE. The main NE owns PANEL, MUXPANEL, two FMUX_32 cards, OLP3_3, and the
main TILA. The protection peer NE owns only the slave and third TILA cards.

`SiteNodeService.createNeXcLink` currently decides whether to run the shared
dual-FMUX64 link builder only from `SiteResourceLayout.usesDualFmux64Topology`.
The same resource layout is carried into the dedicated protection peer, so the
TILA-only peer incorrectly enters the main-NE builder. That builder requires a
PANEL first and fails with:

```text
Cannot find required PANEL occurrence 0 for Bone2.0 Flex64
```

Ordinary protected Flex64 OMSP does not create a dedicated peer NE. Its main
and protection TILA cards remain on the complete endpoint NE, so it does not
exercise this failing path.

## Scope

Apply only to `controller/allocate/designer` on `bytedance`.

The change must preserve:

- unprotected Bone2.0 Flex64 dual-FMUX endpoints;
- ordinary protected Bone2.0 Flex64 OMSP endpoints;
- the full main NE of Bone2.0 Flex64 OMSP 1:3;
- Bone2.0 Flex32 and fixed-grid layouts;
- non-Bone2.0 products;
- existing explicit failure when a full dual-FMUX main NE is missing PANEL,
  MUXPANEL, either FMUX_32 card, TILA, or the required OLP3_3.

No web, NMS, YANG, JSON resource model, persistence schema, telemetry, or
deployed data change is included.

## Design

Retain `protectionPeerRole` in `NodeTp` as allocation context.

1. Add a nullable `RoutingType protectionPeerRole` field to `NodeTp`.
2. Populate the field in `SiteNodeService.createNeEquipAndTp` from the role
   already passed through equipment and TP creation.
3. In `SiteNodeService.createNeXcLink`, run `createDualFmux64Links` and skip the
   generic main-card loop only when both conditions are true:
   - `usesDualFmux64Topology()` is true;
   - `protectionPeerRole == null`.
4. A non-null Slave or Third role therefore follows the generic TILA-only path
   and never requests PANEL or FMUX cards.

The role check is the semantic boundary. The implementation must not infer a
peer from the current card list, because a malformed main NE must continue to
fail rather than silently falling back to a reduced topology.

## Expected Data Flow

For the main route endpoint:

```text
SiteNodeInput(role=null)
  -> NodeTp(role=null, full card chain)
  -> dual-FMUX64 builder
  -> PANEL/MUXPANEL/FMUX/OLP/TILA links
```

For Slave and Third endpoint peers:

```text
SiteNodeInput(role=Slave|Third)
  -> NodeTp(role=Slave|Third, TILA-only card chain)
  -> generic single-card path
  -> no dual-FMUX main-NE lookup
```

The existing `SiteRepo` logic continues to attach the Slave and Third route
legs to OLP ports 1B and 1C on the full main NE.

## Failure Handling

- A complete main node with an invalid or incomplete dual-FMUX card chain must
  still throw the existing required-card `NeDesignerException`.
- A correctly formed TILA-only protection peer must not throw a missing-PANEL
  exception.
- No fallback based on card presence is allowed.

## Tests

Use strict red-green TDD.

1. Add a regression test that builds a Bone2.0 Flex64 OMSP 1:3 TILA-only
   `NodeTp` with `protectionPeerRole=Slave`, invokes the real node XC/link path,
   and proves it does not request PANEL.
2. Verify the test fails before the production change with the exact
   missing-PANEL cause.
3. Add or retain an assertion that a role-null dual-FMUX main node missing
   PANEL still fails explicitly.
4. Run the focused `SiteNodeServiceTest`, `NodeTpTest`,
   `SiteResourceLayoutFactoryTest`, and `SiteRepoTest` suite.
5. Run the complete `alloc-designer` test suite after the focused tests pass.

## Deployment Boundary

The change affects newly designed or recreated Bone2.0 Flex64 OMSP 1:3
SiteLinks. It does not mutate existing SiteLinks, physical nodes, or MongoDB
documents. Runtime behavior is not considered verified until the rebuilt
allocate service is deployed and the failing 112 request is retried.
