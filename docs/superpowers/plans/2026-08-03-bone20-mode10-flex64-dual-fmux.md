# Bone2.0 Mode 10 Flex64 Dual-FMUX Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Make newly designed Bone2.0 flexible-grid mode-10 64-channel SiteLinks use the proven dual-FMUX64 endpoint fanout before the OLP3_3 three-leg split.

**Architecture:** Reuse `SiteNodeService`'s slot-based dual-FMUX64 builder. The mode-10 layout opts into that builder only at 64 channels; the builder detects OLP3_3, connects slot-3 FMUX through OLP `1SIG/1A` to the main TILA, and returns OLP as the protected-card boundary for the existing `1B/1C` slave and third paths.

**Tech Stack:** Java 8, Maven, JUnit 5, OpenDaylight generated topology bindings.

## Global Constraints

- Target repository and branch: `controller/bytedance`.
- Change only Bone2.0, C-band, flexible-grid, `ProtectionBidir1To2`, bandwidth 64 behavior.
- Preserve 32-channel mode-10 and all unprotected/1:1/fixed-grid behavior.
- Do not change YANG, persistence schemas, web, telemetry, or deployed SiteLink data.
- Select main and extension FMUX by physical slot 3 and slot 7, never list order.
- Preserve Java 8 compatibility.

---

### Task 1: Select the dual-FMUX contract for mode-10 Flex64

**Files:**
- Modify: `allocate/designer/src/test/java/net/flex/dci/otn/controller/allocate/designer/site/SiteResourceLayoutFactoryTest.java:65-75`
- Modify: `allocate/designer/src/main/java/net/flex/dci/otn/controller/allocate/designer/site/ByteDance2OneToTwoResourceLayout.java:29-44`

**Interfaces:**
- Consumes: `ByteDance2OneToTwoResourceLayout(int bandwidth)` and existing package-private `SiteResourceLayout` hooks.
- Produces: bandwidth-gated implementations of `usesDualFmux64Topology()`, `useFmux32Com1ProtectionXc(String)`, `hasCardSelfLinks(String)`, and `selectCardSelfLinks(Map,String)`.

- [ ] **Step 1: Extend the layout test with literal 64/32 expectations**

Add assertions to `bandwidth64AddsSecondFmuxAtSlotSeven` and a new 32-channel guard:

```java
assertTrue(layout.usesDualFmux64Topology());
assertTrue(layout.useFmux32Com1ProtectionXc("FMUX_32"));
assertTrue(layout.hasCardSelfLinks("FMUX_32"));

Map<String, List<ExternalLinkTo>> relations = new LinkedHashMap<>();
relations.put("SIG", Arrays.asList(
        target("FMUX_32", "COM1"), target("FMUX_32", "COM2")));
Map<String, List<ExternalLinkTo>> selected =
        layout.selectCardSelfLinks(relations, "FMUX_32");
assertEquals(1, selected.get("SIG").size());
assertEquals("COM2", selected.get("SIG").get(0).getPort());

SiteResourceLayout flex32 = SiteResourceLayoutFactory.create(input(32,
        ProtectionBidir1To2.class));
assertFalse(flex32.usesDualFmux64Topology());
assertFalse(flex32.useFmux32Com1ProtectionXc("FMUX_32"));
assertFalse(flex32.hasCardSelfLinks("FMUX_32"));
```

Production mutation caught: leaving mode-10 64 on the generic sequential-card builder, or accidentally changing 32-channel behavior.

- [ ] **Step 2: Run the focused test and verify RED**

Run:

```powershell
mvn -pl allocate/designer -Dtest=SiteResourceLayoutFactoryTest test
```

Expected: FAIL because the 64-channel layout inherits `usesDualFmux64Topology() == false` and the FMUX protection/self-link hooks are disabled.

- [ ] **Step 3: Implement the bandwidth-gated layout hooks**

In `ByteDance2OneToTwoResourceLayout`, add `FMUX_CARD_TYPE` and override the hooks without changing the existing 32-channel card chain:

```java
private static final int FLEX64_BANDWIDTH = 64;
private static final String FMUX_CARD_TYPE = "FMUX_32";

@Override
boolean usesDualFmux64Topology() {
    return bandwidth == FLEX64_BANDWIDTH;
}

@Override
boolean useFmux32Com1ProtectionXc(String cardType) {
    return usesDualFmux64Topology() && FMUX_CARD_TYPE.equals(cardType);
}

@Override
boolean hasCardSelfLinks(String cardType) {
    return usesDualFmux64Topology() && FMUX_CARD_TYPE.equals(cardType);
}

@Override
Map<String, List<ExternalLinkTo>> selectCardSelfLinks(
        Map<String, List<ExternalLinkTo>> fromToMap, String cardType) {
    Map<String, List<ExternalLinkTo>> selected = new LinkedHashMap<>();
    if (!hasCardSelfLinks(cardType) || !fromToMap.containsKey("SIG")) {
        return selected;
    }
    List<ExternalLinkTo> targets = fromToMap.get("SIG").stream()
            .filter(target -> FMUX_CARD_TYPE.equals(target.getCardType()))
            .filter(target -> "COM2".equals(target.getPort()))
            .collect(Collectors.toList());
    if (!targets.isEmpty()) {
        selected.put("SIG", targets);
    }
    return selected;
}
```

- [ ] **Step 4: Run the focused test and verify GREEN**

Run the Task 1 command again. Expected: all `SiteResourceLayoutFactoryTest` tests pass.

- [ ] **Step 5: Commit the isolated layout contract**

```powershell
git add -- allocate/designer/src/main/java/net/flex/dci/otn/controller/allocate/designer/site/ByteDance2OneToTwoResourceLayout.java allocate/designer/src/test/java/net/flex/dci/otn/controller/allocate/designer/site/SiteResourceLayoutFactoryTest.java
git diff --cached --check
git commit -m "fix: select dual FMUX topology for mode10 Flex64"
```

---

### Task 2: Route the shared dual-FMUX fanout through OLP3_3

**Files:**
- Modify: `allocate/designer/src/test/java/net/flex/dci/otn/controller/allocate/designer/site/SiteNodeServiceTest.java:128-221`
- Modify: `allocate/designer/src/main/java/net/flex/dci/otn/controller/allocate/designer/site/SiteNodeService.java:231-268`

**Interfaces:**
- Consumes: the Task 1 `usesDualFmux64Topology()` contract and existing `createSelectedLinks(...)`/`createNodeSlaveLinks(...)` behavior.
- Produces: `createDualFmux64Links(...)` returns slot-3 FMUX for point-to-point layouts and OLP3_3 for mode-10 layouts.

- [ ] **Step 1: Add a real-link regression fixture**

Use the real `COHERENT-CHASSIS2.0-ByteDance-OD-card.json`, `NeInfo`, `LinkService`, and `LinkRepo`. Invoke `createDualFmux64Links` through reflection with literal TP maps for PANEL, MUXPANEL64, slot-3 FMUX, slot-7 FMUX, OLP3_3, and TILA.

The test must assert actual returned `Link` endpoint IDs, not mock invocations:

```java
assertLink(links, "MUX-1-50#PORT-1-50-MPO1", "LINECARD-1-3#PORT-1-3-MPO1");
assertLink(links, "MUX-1-50#PORT-1-50-MPO4", "LINECARD-1-3#PORT-1-3-MPO4");
assertLink(links, "MUX-1-50#PORT-1-50-MPO5", "LINECARD-1-7#PORT-1-7-MPO1");
assertLink(links, "MUX-1-50#PORT-1-50-MPO8", "LINECARD-1-7#PORT-1-7-MPO4");
assertLink(links, "LINECARD-1-7#PORT-1-7-SIG", "LINECARD-1-3#PORT-1-3-COM1");
assertLink(links, "LINECARD-1-3#PORT-1-3-SIG", "LINECARD-1-3#PORT-1-3-COM2");
assertLink(links, "LINECARD-1-3#PORT-1-3-SIGA", "LINECARD-1-1#PORT-1-1-1SIG");
assertLink(links, "LINECARD-1-1#PORT-1-1-1A", "LINECARD-1-5#PORT-1-5-LINE_WEST");
assertSame(olp, protectedCard);
```

Run the same fixture with `reversed=false` and `reversed=true`; both must contain the same undirected endpoint pairs and return OLP. Production mutations caught: omitting MPO5..8, swapping slot roles, bypassing OLP, or returning main FMUX and breaking `1B/1C` creation.

- [ ] **Step 2: Add the slot-3 APS regression**

Extend `protectsOnlySlotThreeFmuxInDualFmux64Layout` to invoke `createNodeXcs` with `new ByteDance2OneToTwoResourceLayout(64)`. Assert exactly one COM-based APS XC and that its TP prefix contains `LINECARD-1-3`; repeat with the input FMUX list reversed.

- [ ] **Step 3: Run the focused tests and verify RED**

Run:

```powershell
mvn -pl allocate/designer -Dtest=SiteNodeServiceTest,SiteResourceLayoutFactoryTest test
```

Expected: the link fixture fails because the current builder connects slot-3 FMUX directly to TILA and returns main FMUX instead of traversing/returning OLP.

- [ ] **Step 4: Add optional OLP handling to the shared builder**

Add a nullable card lookup:

```java
private CardTps getOptionalCard(List<CardTps> cards, String cardType) {
    return cards.stream()
            .filter(card -> cardType.equals(card.getCard().getCardType()))
            .findFirst()
            .orElse(null);
}
```

In `createDualFmux64Links`, resolve `OLP3_3`. Preserve the existing panel/MPO/dual-FMUX creation and replace only the line-facing connection:

```java
CardTps olp = getOptionalCard(cardTpsList, "OLP3_3");
CardTps protectedCard;
if (olp == null) {
    createSelectedLinks(neInfo, nodeId, mainFmux, tila,
            mainFmuxLinePort, "LINE_WEST", 1,
            links, internalLinks, busyIds, false);
    protectedCard = mainFmux;
} else {
    createSelectedLinks(neInfo, nodeId, mainFmux, olp,
            mainFmuxLinePort, "1SIG", 1,
            links, internalLinks, busyIds, false);
    createSelectedLinks(neInfo, nodeId, olp, tila,
            "1A", "LINE_WEST", 1,
            links, internalLinks, busyIds, false);
    protectedCard = olp;
}
return protectedCard;
```

Extract the line-facing block into
`connectDualFmux64Line(SiteResourceLayout, NeInfo, String, CardTps, CardTps,
CardTps, String, List<Link>, List<InternalLinks>, Set<String>)`. Call that
method where the current direct `mainFmux -> tila` operation appears in each
construction order. The method returns OLP when OLP is present and main FMUX
otherwise. Do not change slot selection or expected link counts.

- [ ] **Step 5: Run the focused tests and verify GREEN**

Run the Task 2 command again. Expected: all selected tests pass with zero failures/errors.

- [ ] **Step 6: Commit the OLP-aware fanout**

```powershell
git add -- allocate/designer/src/main/java/net/flex/dci/otn/controller/allocate/designer/site/SiteNodeService.java allocate/designer/src/test/java/net/flex/dci/otn/controller/allocate/designer/site/SiteNodeServiceTest.java
git diff --cached --check
git commit -m "fix: connect mode10 Flex64 dual FMUX through OLP"
```

---

### Task 3: Proportional regression verification

**Files:**
- Verify only; no planned production edits.

**Interfaces:**
- Consumes: Tasks 1 and 2 commits.
- Produces: focused and module-level evidence for the changed designer topology behavior.

- [ ] **Step 1: Run all directly related tests**

```powershell
mvn -pl allocate/designer -Dtest=SiteResourceLayoutFactoryTest,SiteNodeServiceTest,NodeTpTest,NeInfoTest test
```

Expected: all selected tests pass.

- [ ] **Step 2: Run the designer module suite**

```powershell
mvn -pl allocate/designer test
```

Expected: pass, or report any known unrelated baseline failure separately with its exact test and message.

- [ ] **Step 3: Check scope and whitespace**

```powershell
git status --short
git diff HEAD~2 --check
git diff HEAD~2 --name-only
```

Expected changed implementation scope: the two production Java files and two test files, plus the approved spec/plan documents. Existing untracked files remain untouched.

---

### Task 4: Close the DCI knowledge loop

**Files:**
- Update via workflow: `DCI-Knowledge/04-Operations/SiteLink-Mongo-Inspection.md`
- Update via workflow: generated contracts, snapshot, and change history.

**Interfaces:**
- Consumes: verified implementation and test output.
- Produces: closed `.dci-agent/current-change.json` state and refreshed durable project knowledge.

- [ ] **Step 1: Record the corrected mode-10 Flex64 contract**

Document that mode-10 64-channel endpoints reuse the dual-FMUX fanout, slot 3 is main/protection, slot 7 is extension, and OLP3_3 owns the `1A/1B/1C` split. Do not include live customer IDs or credentials.

- [ ] **Step 2: Finish the tracked change**

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\finish-dci-change.ps1 `
  -Summary "Reuse dual FMUX64 fanout for Bone2.0 mode10 before OLP three-leg split" `
  -Verification "Focused layout/node tests and designer module test results"
```

- [ ] **Step 3: Verify closure**

Confirm `.dci-agent/current-change.json` is absent, inspect refreshed knowledge files, and verify no unrelated worktree path was staged or committed.
