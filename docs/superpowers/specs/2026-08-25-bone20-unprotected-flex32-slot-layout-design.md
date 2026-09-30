# Bone2.0 Unprotected Flex32 Slot Layout Design

## Problem and Evidence

The 114 environment created this unprotected Bone2.0 flexible-grid 32-channel
SiteLink with the two endpoint equipment nodes reversed:

```text
SiteLink-Site-2091744185781719040#Ne-2092000410406621184#MUX-1-50#PORT-1-50-MPO-Site-2091744186251481088#Ne-2092000410469535744#MUX-1-50#PORT-1-50-MPO
```

Both configured endpoint nodes place `FMUX_32` in slot 1 and `TILA` in slot 3.
The physical hardware contract is the opposite: slot 1 is `TILA`, and slot 3
is `FMUX_32`. The device rejects the generated NETCONF configuration because
it cannot change `LINECARD-1-1` from preconfigured `TILA` to `FMUX_32`.

The SiteLink was created after the currently running allocate JAR started, and
the running source version already contains the current layout framework. This
rules out stale allocation data as the cause.

`SiteResourceLayoutFactory` has exact strategies for Bone2.0 Flex64 and
protected layouts. A C-band, `grid=0`, bandwidth-32, unprotected request misses
those selectors and falls through to `ByteDance2DefaultResourceLayout`.
That layout preserves the correct Bone2.0 endpoint orientation but inherits a
null `getMainNodeSlot`. `SiteNodeService` therefore assigns the card chain to
the next available slots, producing `FMUX_32@1` and `TILA@3`.

## Scope

Apply only to `controller/allocate/designer` on the confirmed `bytedance`
target branch.

The change covers only requests matching all of these properties:

- Bone2.0 product type;
- C band;
- flexible grid (`grid=0`);
- bandwidth 32;
- `ProtectionUnprotected`.

The change must preserve:

- the existing site-model-driven main card classes and card-chain order;
- `FMUX_32 SIG -> TILA LINE_WEST` as the internal optical link;
- `TILA LINE_EAST` as the whole-NE external optical exit;
- TILA class-mode support and current A/Z endpoint orientation;
- Bone2.0 Flex64 layouts;
- protected Bone2.0 Flex32 layouts;
- fixed-grid layouts;
- non-Bone2.0 products.

No web, platform, telemetry, YANG, API, persistence-schema, MongoDB, device, or
deployed-data change is included.

## Selected Design

Add a dedicated `ByteDance2Flex32UnprotectedResourceLayout` and an exact
selector in `SiteResourceLayoutFactory`.

The new layout extends `ByteDance2DefaultResourceLayout` so all current
site-model, port, XC, endpoint-orientation, and class-mode behavior remains the
same. `ByteDance2DefaultResourceLayout` is currently a package-private final
class. Remove only its `final` modifier to permit this package-local
specialization; do not change any of its methods or visibility. The new layout
overrides only `getMainNodeSlot`:

| Card type | Occurrence | Slot |
| --- | ---: | ---: |
| `TILA` | 0 | 1 |
| `FMUX_32` | 0 | 3 |

Unknown card types or later occurrences return null and retain the existing
site-model-driven fallback. This keeps the patch narrow and avoids inventing a
hardware contract outside the observed Flex32 endpoint.

The selector predicate is exact:

```text
isBone20ProductType(vendorName, vendorType)
&& wdmBand == C
&& grid == 0
&& bandwidth == 32
&& protectionType == ProtectionUnprotected
```

The selector is evaluated before the general Bone2.0 default fallback. It does
not replace or broaden the Flex64 selector.

## Alternatives Rejected

### Reuse a protected Flex32 layout

Protected layouts encode OLP cards, protection-node behavior, and different
slot maps. Reusing one would mix protection topology into an unprotected
request and change more than the two incorrect slots.

### Change `ByteDance2DefaultResourceLayout` globally

That default handles other Bone2.0 combinations. Giving it unconditional
Flex32 slots would alter fixed-grid and unsupported combinations without a
verified hardware contract.

### Reorder the generated card chain

Changing card order would make physical placement depend on incidental JSON or
iteration order and could alter XC creation. Slots are a hardware invariant
and belong in the resource-layout strategy.

## Data Flow

```text
SiteInput(Bone2.0, C, grid=0, bandwidth=32, unprotected)
  -> SiteResourceLayoutFactory exact selector
  -> ByteDance2Flex32UnprotectedResourceLayout
  -> existing site-model card classes and link/XC construction
  -> getMainNodeSlot(TILA, 0) = 1
  -> getMainNodeSlot(FMUX_32, 0) = 3
  -> endpoint node: TILA-1-1 + FMUX_32-1-3
```

Port semantics remain unchanged:

```text
FMUX_32 SIG <-> TILA LINE_WEST    internal MUX-facing connection
TILA LINE_EAST                    external whole-NE optical exit
```

## Compatibility and Failure Behavior

- A matching Flex32 request receives deterministic hardware slots.
- A nonmatching request follows its current selector or default layout.
- The layout does not silently rewrite unsupported card occurrences.
- Existing required-card and slot-collision failures remain unchanged.
- No fallback may infer TILA/FMUX roles from A/Z direction or collection order.

## Implementation Locations

Create:

- `allocate/designer/src/main/java/net/flex/dci/otn/controller/allocate/designer/site/ByteDance2Flex32UnprotectedResourceLayout.java`

Modify:

- `allocate/designer/src/main/java/net/flex/dci/otn/controller/allocate/designer/site/ByteDance2DefaultResourceLayout.java`
- `allocate/designer/src/main/java/net/flex/dci/otn/controller/allocate/designer/site/SiteResourceLayoutFactory.java`
- `allocate/designer/src/test/java/net/flex/dci/otn/controller/allocate/designer/site/SiteResourceLayoutFactoryTest.java`
- `allocate/designer/src/test/java/net/flex/dci/otn/controller/allocate/designer/site/SiteNodeServiceTest.java`

No other repository is expected to change.

## Tests

Use strict red-green TDD.

1. Change the existing unprotected Bone2.0 Flex32 factory regression so it
   fails against the current implementation and proves the selected layout
   returns `TILA@1` and `FMUX_32@3` while preserving its inherited behavior.
2. Add an end-to-end `SiteNodeServiceTest` assertion on the generated equipment
   placement, proving the node contains `TILA-1-1` and `FMUX_32-1-3`.
3. Assert the existing internal/external endpoint contract remains
   `FMUX_32 SIG <-> TILA LINE_WEST` and `TILA LINE_EAST` externally.
4. Retain or add boundary cases proving fixed-grid bandwidth 32, protected
   bandwidth 32, Bone2.0 Flex64, and non-Bone2.0 requests do not select the new
   layout.
5. Run focused `SiteResourceLayoutFactoryTest` and `SiteNodeServiceTest` tests,
   then run the complete `alloc-designer` test suite.

The pre-change factory baseline is 13 tests with zero failures or errors.

## Deployment and Existing-Data Boundary

The code change affects only newly designed or recreated matching SiteLinks.
It does not mutate the existing bad 114 SiteLink, its physical nodes, device
preconfiguration, or MongoDB documents.

After the rebuilt allocate service is deployed, the named 114 SiteLink must be
recreated or reconciled through the approved operational workflow. Runtime
acceptance requires read-back evidence from both endpoint configuration nodes
showing `TILA-1-1` and `FMUX_32-1-3`, followed by a successful device
configuration attempt. That operational repair is a separate approval and is
not part of this code change.

## Acceptance Criteria

- The exact matching request selects the dedicated unprotected Flex32 layout.
- Both endpoint nodes deterministically allocate `TILA` to slot 1 and
  `FMUX_32` to slot 3.
- Internal and external TILA port roles are unchanged.
- Boundary scenarios continue selecting their previous layouts.
- Focused and full `alloc-designer` tests pass.
- Only the five listed controller paths, plus required DCI workflow metadata or
  knowledge records, change during implementation.
