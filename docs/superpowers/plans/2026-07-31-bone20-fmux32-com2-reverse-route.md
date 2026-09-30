# Bone2.0 FMUX32 COM2 And Reverse Route Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Correct Bone2.0 Flex32 to use COM2 and serialize a continuous reverse D-end ERO with a complete virtual MPO-SIG XC ID.

**Architecture:** Keep the card JSON unchanged because it already declares both COM self-links. Select COM2 in the Flex32 layout and APS adaptation. Extend SiteLink ERO construction with a small same-equipment ambiguity resolver that consults the route leg's XC endpoints, and rebuild aggregated XC IDs from their collapsed endpoints.

**Tech Stack:** Java 8, JUnit 5, generated YANG topology classes, Maven.

## Global Constraints

- Target `controller/bytedance`, on top of commit `17c66bf81`.
- Controller only; no frontend or platform changes.
- Fixed chain: `MPO -> SIG -> COM2`; APS: `COM2 -> SIGA,SIGB`.
- Preserve equipment, TP, physical-link and protection identifiers.
- No historical data migration.
- Do not commit or push unless explicitly requested.

---

### Task 1: Select COM2 For Flex32

**Files:**
- Modify: `allocate/designer/src/main/java/net/flex/dci/otn/controller/allocate/designer/site/ByteDance2FlexOneToOneResourceLayout.java`
- Modify: `allocate/designer/src/main/java/net/flex/dci/otn/controller/allocate/designer/site/SiteNodeService.java`
- Modify: `allocate/designer/src/test/java/net/flex/dci/otn/controller/allocate/designer/site/SiteResourceLayoutFactoryTest.java`
- Modify: `allocate/designer/src/test/java/net/flex/dci/otn/controller/allocate/designer/site/SiteNodeServiceTest.java`

**Interfaces:**
- Produces: Flex32 self-link selection `SIG -> COM2` and APS adaptation `COM2 -> SIGA,SIGB`.

- [ ] Add a failing layout test with SIG targets for both COM1 and COM2; assert `selectCardSelfLinks(..., "FMUX_32")` returns only COM2.
- [ ] Change `ByteDance2FlexOneToOneResourceLayout.selectCardSelfLinks` target filter from `COM1` to `COM2` and update its comment.
- [ ] Change `SiteNodeService.adaptFmux32ProtectionXc` source port from `COM1` to `COM2`.
- [ ] Update `rewritesOnlyFmux32ApsCrossConnection` to expect COM2, and update the faithful node-path fixture to supply COM2.
- [ ] Run:

```powershell
mvn -pl allocate/designer '-Dtest=SiteResourceLayoutFactoryTest,SiteNodeServiceTest' test
```

Expected: all selected tests pass; the tests must fail against the current COM1 implementation before production edits.

### Task 2: Orient Same-Equipment Links Through XCs

**Files:**
- Modify: `allocate/manager/src/main/java/net/flex/dci/otn/controller/allocate/link/common/Route.java`
- Modify: `allocate/manager/src/test/java/net/flex/dci/otn/controller/allocate/link/common/RouteTest.java`

**Interfaces:**
- Produces: a package-private/static testable resolver that chooses a same-equipment link endpoint connected to the current TP by one route XC.
- Consumes: the primary/slave/third leg's physical XC list.

- [ ] Add failing resolver tests for:

```text
A: current=MPO1, candidates=SIG/COM2, XC=MPO1..4<->SIG => choose SIG
D: current=SIGA, candidates=SIG/COM2, XC=COM2<->SIGA,SIGB => choose COM2
```

- [ ] Pass each leg's XC list from `getExplictRoute` through `buildRoute` into `buildPathRoutObject`.
- [ ] In `buildPathRoutObject`, preserve exact endpoint matching first. Only when both candidate endpoints belong to the current equipment and neither equals the current TP, select the candidate that shares one XC with the current TP. Fall back to existing equipment-ID behavior if no XC proves adjacency.
- [ ] Keep direction handling generic; do not check FMUX32, COM2, A-side or D-side names in production code.
- [ ] Run the focused resolver tests through the documented isolated Java 8 manager route if normal manager compilation remains blocked by unchanged `TunnelBinder` generated classes.

Expected D sequence:

```text
SIGA -> COM2 -> COM2-SIG link -> SIG -> MPO
```

### Task 3: Preserve Both Endpoints In Aggregated XC IDs

**Files:**
- Modify: `allocate/manager/src/main/java/net/flex/dci/otn/controller/allocate/link/common/Route.java`
- Modify: `allocate/manager/src/test/java/net/flex/dci/otn/controller/allocate/link/common/RouteTest.java`

**Interfaces:**
- Produces: aggregated bidirectional XC IDs derived from collapsed source and destination TP lists.

- [ ] Change the existing aggregation assertion from:

```text
XC-<FMUX-prefix>MPO
```

to:

```text
XC-<FMUX-prefix>MPO-<FMUX-prefix>SIG
```

- [ ] After collapsing the MPO source/destination list, build the XC ID by concatenating all collapsed TP references, sorting for the bidirectional XC, and joining with `-` after `XC-`.
- [ ] Assert both generated IDs remain unique and each contains exactly the virtual MPO and SIG endpoints.
- [ ] Execute `RouteTest#collapsesFmuxMpoSigFanInForSiteLink` against current `Route.java` using the established isolated Java 8 path if needed.

### Task 4: Verify And Close

**Files:**
- Update: `DCI-Knowledge/04-Operations/SiteLink-Mongo-Inspection.md`

- [ ] Run all affected designer tests and the isolated focused manager tests.
- [ ] Run `git diff --check` and inspect only intended controller files; keep `auth-service/auth-rest/bin/` untouched.
- [ ] Document the corrected `MPO -> SIG -> COM2 -> SIGA/SIGB` chain and D-end reverse ERO.
- [ ] Refresh CodeGraph and close the DCI change with `scripts/finish-dci-change.ps1` after final review.
