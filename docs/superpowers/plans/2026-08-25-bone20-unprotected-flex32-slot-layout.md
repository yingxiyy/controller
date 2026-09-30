# Bone2.0 Unprotected Flex32 Slot Layout Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Make matching Bone2.0 unprotected Flex32 allocations place TILA in slot 1 and FMUX_32 in slot 3 without changing endpoint port semantics or any other layout.

**Architecture:** Add one exact factory selector and one package-local layout specialization. The specialization inherits all current Bone2.0 default site-model, XC, class-mode, and A/Z orientation behavior and overrides only the two verified hardware slots.

**Tech Stack:** Java 8, Maven 3.9.9, JUnit 5, generated OpenDaylight YANG bindings.

## Global Constraints

- Work only in `controller/allocate/designer` on the confirmed `bytedance` target, isolated in `C:\Users\y3446\Documents\DCI\w\c32` on branch `codex/bone20-flex32-slot`.
- Match only Bone2.0, C band, `grid=0`, bandwidth 32, and `ProtectionUnprotected`.
- Preserve site-model card classes and order, `FMUX_32 SIG <-> TILA LINE_WEST`, external `TILA LINE_EAST`, TILA class-mode, and A/Z orientation.
- Do not change platform, telemetry, web, YANG, API, persistence, MongoDB, deployed services, or 114 devices.
- Keep Java 8 compatibility and use strict red-green TDD.
- Maven commands in this linked worktree must include `-Dmaven.gitcommitid.skip=true` because git-commit-id-plugin 4.9.10 cannot resolve linked-worktree metadata.
- Design source: `docs/superpowers/specs/2026-08-25-bone20-unprotected-flex32-slot-layout-design.md`.

---

### Task 1: Reproduce the slot inversion at the layout and generated-equipment boundaries

**Files:**
- Modify: `allocate/designer/src/test/java/net/flex/dci/otn/controller/allocate/designer/site/SiteResourceLayoutFactoryTest.java:169-178`
- Modify: `allocate/designer/src/test/java/net/flex/dci/otn/controller/allocate/designer/site/SiteNodeServiceTest.java:22-38,298-343`

**Interfaces:**
- Consumes: `SiteResourceLayoutFactory.create(SiteInput)`, `SiteResourceLayout.getMainNodeSlot(String, int)`, and `SiteNodeService.createNeEquipAndTp(...)`.
- Produces: regression evidence that the current default layout returns no explicit slots and therefore generates `FMUX_32@1`, `TILA@3` instead of the required `TILA@1`, `FMUX_32@3`.

- [ ] **Step 1: Strengthen the existing factory regression with literal slot and port-role expectations**

Replace `unprotectedBone20RequestKeepsSiteModelLayoutWithEndpointOrientation` with:

```java
@Test
void unprotectedBone20Flex32UsesHardwareSlotsAndKeepsEndpointOrientation() {
    SiteResourceLayout layout = SiteResourceLayoutFactory.create(input(32,
            ProtectionUnprotected.class));

    assertFalse(layout.requiresDedicatedProtectionNode());
    assertTrue(layout instanceof ByteDance2DefaultResourceLayout);
    assertEquals(Arrays.asList("legacy"),
            layout.resolveMainCardClasses(Arrays.asList("legacy")));
    assertEquals(Integer.valueOf(1), layout.getMainNodeSlot("TILA", 0));
    assertEquals(Integer.valueOf(3), layout.getMainNodeSlot("FMUX_32", 0));
    assertEquals(null, layout.getMainNodeSlot("TILA", 1));
    assertEquals(null, layout.getMainNodeSlot("FMUX_32", 1));

    Map<String, List<ExternalLinkTo>> relations = new LinkedHashMap<>();
    relations.put("SIG", Arrays.asList(
            target("TILA", "LINE_EAST"), target("TILA", "LINE_WEST")));
    Map<String, List<ExternalLinkTo>> selected = layout.selectFmuxTilaLinks(
            relations, "FMUX_32", "TILA", "SIG");
    assertEquals(1, selected.get("SIG").size());
    assertEquals("LINE_WEST", selected.get("SIG").get(0).getPort());
}
```

The production mutation this test catches is removing or bypassing the exact
Flex32 selector/slot override. The literal slot expectations are derived from
the 114 hardware contract, not from production helpers.

- [ ] **Step 2: Add a real generated-equipment regression**

Add these imports to `SiteNodeServiceTest`:

```java
import net.flex.dci.otc.common.util.frequency.WDM_Band;
import net.flex.dci.otn.controller.allocate.designer.NodeUtils;
import net.flex.dci.otn.controller.allocate.designer.model.site.SiteInput;
import net.flex.dci.otn.controller.allocate.designer.ne.EquipmentRepo;
import net.flex.dci.otn.controller.allocate.designer.ne.TpRepo;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.ProtectionUnprotected;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.node.TerminationPoint;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.node.attributes.physical.Equipments;
```

Add the test:

```java
@Test
void unprotectedBone20Flex32CreatesTilaInSlotOneAndFmuxInSlotThree() throws Exception {
    SiteResourceLayout layout = SiteResourceLayoutFactory.create(
            bone20UnprotectedFlex32Input());
    SiteNodeService service = serviceWithRealEquipmentRepositories();
    List<Equipments> equipments = new ArrayList<>();

    service.createNeEquipAndTp(0, ProtectionUnprotected.class, WDM_Band.C,
            "Site-A#Ne-A", layout.resolveMainCardClasses(Arrays.asList("CMUX", "ILA")),
            Collections.emptyList(), bone20NeInfo(), Collections.emptySet(), equipments,
            new ArrayList<TerminationPoint>(), layout, null);

    assertEquipmentSlot(equipments, "TILA", "1");
    assertEquipmentSlot(equipments, "FMUX_32", "3");
}
```

Add these test helpers:

```java
private SiteInput bone20UnprotectedFlex32Input() {
    return SiteInput.builder()
            .nodesMap(Collections.emptyMap())
            .vendorName("COHERENT")
            .vendorType("CHASSIS2.0")
            .bandwidth(32)
            .isProtected(false)
            .grid(0)
            .plane("p")
            .planeId("p")
            .riskGroupName("r")
            .linkModel("6")
            .wdmBand(WDM_Band.C)
            .protectionType(ProtectionUnprotected.class)
            .build();
}

private SiteNodeService serviceWithRealEquipmentRepositories() throws Exception {
    JsonYangConverter converter = new JsonYangConverter();
    EquipmentRepo equipmentRepo = new EquipmentRepo();
    setField(equipmentRepo, "jsonYangConverter", converter);
    TpRepo tpRepo = new TpRepo();
    setField(tpRepo, "jsonYangConverter", converter);

    SiteNodeService service = new SiteNodeService();
    setField(service, "nodeUtils", new NodeUtils());
    setField(service, "equipmentRepo", equipmentRepo);
    setField(service, "tpRepo", tpRepo);
    return service;
}

private void assertEquipmentSlot(List<Equipments> equipments, String cardType, String slot) {
    Equipments equipment = equipments.stream()
            .filter(item -> cardType.equals(item.getEquipTypeConfiged()))
            .findFirst()
            .orElseThrow(() -> new AssertionError("Missing equipment " + cardType));
    assertEquals(slot, equipment.getSlot());
}

private void setField(Object target, String fieldName, Object value) throws Exception {
    Field field = target.getClass().getDeclaredField(fieldName);
    field.setAccessible(true);
    field.set(target, value);
}
```

This test uses the real JSON model, `EquipmentRepo`, and `TpRepo`. Reflection
only supplies Spring dependencies; assertions target generated equipment, not
the injected objects.

- [ ] **Step 3: Run both tests and verify the RED state**

Run:

```powershell
mvn -pl allocate/designer -am test "-Dtest=SiteResourceLayoutFactoryTest,SiteNodeServiceTest" "-DfailIfNoTests=false" "-Dcheckstyle.skip=true" "-Dmaven.gitcommitid.skip=true"
```

Expected: test compilation succeeds, then both new slot assertions fail because
the current `ByteDance2DefaultResourceLayout.getMainNodeSlot` path returns null
and allocation uses card order (`FMUX_32@1`, `TILA@3`). Fix test setup errors
until failures are assertion failures caused by those wrong values.

---

### Task 2: Add the exact unprotected Flex32 layout selector and slot strategy

**Files:**
- Create: `allocate/designer/src/main/java/net/flex/dci/otn/controller/allocate/designer/site/ByteDance2Flex32UnprotectedResourceLayout.java`
- Modify: `allocate/designer/src/main/java/net/flex/dci/otn/controller/allocate/designer/site/ByteDance2DefaultResourceLayout.java:8`
- Modify: `allocate/designer/src/main/java/net/flex/dci/otn/controller/allocate/designer/site/SiteResourceLayoutFactory.java:16-18,46`
- Test: `allocate/designer/src/test/java/net/flex/dci/otn/controller/allocate/designer/site/SiteResourceLayoutFactoryTest.java`
- Test: `allocate/designer/src/test/java/net/flex/dci/otn/controller/allocate/designer/site/SiteNodeServiceTest.java`

**Interfaces:**
- Consumes: existing package-local `ByteDance2DefaultResourceLayout` behavior and factory `LayoutSelector` convention.
- Produces: `ByteDance2Flex32UnprotectedResourceLayout`, whose only new contract is `getMainNodeSlot("TILA", 0) == 1` and `getMainNodeSlot("FMUX_32", 0) == 3`.

- [ ] **Step 1: Permit package-local specialization without changing default behavior**

In `ByteDance2DefaultResourceLayout.java`, change only:

```java
final class ByteDance2DefaultResourceLayout extends DefaultSiteResourceLayout {
```

to:

```java
class ByteDance2DefaultResourceLayout extends DefaultSiteResourceLayout {
```

Keep the class package-private and leave all existing method bodies unchanged.

- [ ] **Step 2: Add the minimal slot-only layout**

Create `ByteDance2Flex32UnprotectedResourceLayout.java`:

```java
package net.flex.dci.otn.controller.allocate.designer.site;

/** Bone2.0 unprotected flexible-grid 32-channel physical slot contract. */
final class ByteDance2Flex32UnprotectedResourceLayout
        extends ByteDance2DefaultResourceLayout {

    @Override
    Integer getMainNodeSlot(String cardType, int occurrence) {
        if (occurrence != 0) {
            return null;
        }
        if ("TILA".equals(cardType)) {
            return 1;
        }
        if ("FMUX_32".equals(cardType)) {
            return 3;
        }
        return null;
    }
}
```

- [ ] **Step 3: Register an exact selector before the default fallback**

Add `new ByteDance2Flex32UnprotectedLayoutSelector()` to `SELECTORS` without
changing the Flex64 or one-to-two selectors. Add:

```java
private static final class ByteDance2Flex32UnprotectedLayoutSelector
        implements LayoutSelector {

    @Override
    public boolean supports(SiteInput input) {
        return input != null
                && ProductTypeResolver.isBone20ProductType(
                        input.getVendorName(), input.getVendorType())
                && input.getWdmBand() == WDM_Band.C
                && input.getGrid() == 0
                && Integer.valueOf(32).equals(input.getBandwidth())
                && ProtectionUnprotected.class.equals(input.getProtectionType());
    }

    @Override
    public SiteResourceLayout create(SiteInput input) {
        return new ByteDance2Flex32UnprotectedResourceLayout();
    }
}
```

- [ ] **Step 4: Run the focused tests and verify the GREEN state**

Run the same focused command:

```powershell
mvn -pl allocate/designer -am test "-Dtest=SiteResourceLayoutFactoryTest,SiteNodeServiceTest" "-DfailIfNoTests=false" "-Dcheckstyle.skip=true" "-Dmaven.gitcommitid.skip=true"
```

Expected: both test classes pass. Confirm the test counts and zero failures or
errors from Maven output.

- [ ] **Step 5: Add selector-boundary assertions while green**

Add a factory test that proves nonmatching requests retain their exact current
classes and no explicit slots:

```java
@Test
void flex32SlotLayoutDoesNotApplyOutsideExactScenario() {
    SiteResourceLayout fixedGrid = SiteResourceLayoutFactory.create(input(32,
            ProtectionUnprotected.class, 75));
    SiteResourceLayout protectedFlex = SiteResourceLayoutFactory.create(input(32,
            ProtectionBidir1To1.class));
    SiteInput nonBone20Input = input(32, ProtectionUnprotected.class);
    nonBone20Input.setVendorName("OTHER");
    nonBone20Input.setVendorType("OTHER");
    SiteResourceLayout nonBone20 = SiteResourceLayoutFactory.create(nonBone20Input);

    assertEquals(ByteDance2DefaultResourceLayout.class, fixedGrid.getClass());
    assertEquals(null, fixedGrid.getMainNodeSlot("TILA", 0));
    assertTrue(protectedFlex instanceof ByteDance2FlexOneToOneResourceLayout);
    assertEquals(Integer.valueOf(1), protectedFlex.getMainNodeSlot("TILA", 0));
    assertEquals(DefaultSiteResourceLayout.class, nonBone20.getClass());
    assertEquals(null, nonBone20.getMainNodeSlot("TILA", 0));
}
```

Existing factory tests remain the boundary evidence for Bone2.0 Flex64.

- [ ] **Step 6: Re-run focused tests after the boundary assertions**

Run the focused Maven command from Step 4 again. Expected: all focused tests
pass with zero failures and errors.

- [ ] **Step 7: Review and commit the implementation atomically**

Run:

```powershell
git diff --check
git diff -- allocate/designer/src/main/java/net/flex/dci/otn/controller/allocate/designer/site/ByteDance2DefaultResourceLayout.java allocate/designer/src/main/java/net/flex/dci/otn/controller/allocate/designer/site/ByteDance2Flex32UnprotectedResourceLayout.java allocate/designer/src/main/java/net/flex/dci/otn/controller/allocate/designer/site/SiteResourceLayoutFactory.java allocate/designer/src/test/java/net/flex/dci/otn/controller/allocate/designer/site/SiteResourceLayoutFactoryTest.java allocate/designer/src/test/java/net/flex/dci/otn/controller/allocate/designer/site/SiteNodeServiceTest.java
```

Verify there are no unrelated changes, then stage only those five files and
commit:

```powershell
git add -- allocate/designer/src/main/java/net/flex/dci/otn/controller/allocate/designer/site/ByteDance2DefaultResourceLayout.java allocate/designer/src/main/java/net/flex/dci/otn/controller/allocate/designer/site/ByteDance2Flex32UnprotectedResourceLayout.java allocate/designer/src/main/java/net/flex/dci/otn/controller/allocate/designer/site/SiteResourceLayoutFactory.java allocate/designer/src/test/java/net/flex/dci/otn/controller/allocate/designer/site/SiteResourceLayoutFactoryTest.java allocate/designer/src/test/java/net/flex/dci/otn/controller/allocate/designer/site/SiteNodeServiceTest.java
git commit -m "fix: map unprotected Flex32 cards to hardware slots"
```

---

### Task 3: Verify the module and close the DCI change record

**Files:**
- Verify: all files under `allocate/designer` affected by compilation and tests.
- Refresh: DCI workflow-generated contract, CodeGraph, project snapshot, and change-history artifacts created by `scripts/finish-dci-change.ps1`.

**Interfaces:**
- Consumes: committed slot-layout implementation from Task 2.
- Produces: fresh full-module verification evidence and a closed DCI change record; it does not produce deployment or live-114 evidence.

- [ ] **Step 1: Run the complete alloc-designer test suite**

Run:

```powershell
mvn -pl allocate/designer -am test "-DfailIfNoTests=false" "-Dcheckstyle.skip=true" "-Dmaven.gitcommitid.skip=true"
```

Expected: reactor `BUILD SUCCESS`, zero failures and errors. Record exact test
counts from the output rather than extrapolating from focused tests.

- [ ] **Step 2: Request an independent code review**

Review the implementation commit against
`docs/superpowers/specs/2026-08-25-bone20-unprotected-flex32-slot-layout-design.md`.
Treat Critical and Important findings as blockers; apply corrections through a
new red-green test cycle and rerun focused plus full tests.

- [ ] **Step 3: Run final diff and repository-boundary checks**

Run:

```powershell
git status --short
git diff origin/bytedance...HEAD --stat
git log --oneline --decorate -3
```

Expected: only the design, plan, five implementation/test paths, and workflow
knowledge artifacts are present. Do not stage unrelated files.

- [ ] **Step 4: Close the DCI workflow record**

From `C:\Users\y3446\Documents\DCI`, run:

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\finish-dci-change.ps1 -Summary "Add exact Bone2.0 unprotected Flex32 layout with TILA slot 1 and FMUX_32 slot 3" -Verification "Focused SiteResourceLayoutFactoryTest and SiteNodeServiceTest plus full alloc-designer Maven test suite passed"
```

Inspect all generated changes, ensure no secrets or customer payloads were
recorded, and verify `.dci-agent/current-change.json` is no longer active.

- [ ] **Step 5: Report the deployment boundary**

State explicitly that source and module tests are complete but the running 114
allocate service and the named SiteLink remain unchanged. Deployment and
recreate/reconcile require separate operational approval and live read-back.
