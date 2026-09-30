# Bone2.0 Flex64 OMSP 1:3 Protection Peer Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Prevent Bone2.0 Flex64 OMSP 1:3 TILA-only Slave and Third peer nodes from entering the full dual-FMUX main-node builder while preserving strict validation for real main nodes.

**Architecture:** Carry the existing nullable `RoutingType protectionPeerRole` into `NodeTp` and make it part of the dual-FMUX branch predicate in `SiteNodeService.createNeXcLink`. The resource layout continues to describe the complete OMSP 1:3 topology; the node role identifies whether the current allocation is the full main NE or a dedicated protection peer.

**Tech Stack:** Java 8, Lombok `@Builder`/`@Data`, JUnit 5, Maven Surefire.

## Global Constraints

- Target repository and branch: `controller/bytedance`.
- Production scope is limited to `allocate/designer`.
- Preserve unprotected and ordinary protected Bone2.0 Flex64 dual-FMUX behavior.
- Preserve the full Bone2.0 Flex64 OMSP 1:3 main-node PANEL/MUXPANEL/FMUX/OLP/TILA topology.
- Preserve explicit missing-card failures for role-null dual-FMUX main nodes.
- Do not modify web, NMS, YANG, JSON assets, persistence, telemetry, or deployed data.
- Use strict red-green TDD and Java 8-compatible syntax.

---

### Task 1: Make dual-FMUX selection protection-peer aware

**Files:**
- Modify: `allocate/designer/src/test/java/net/flex/dci/otn/controller/allocate/designer/site/SiteNodeServiceTest.java`
- Modify: `allocate/designer/src/main/java/net/flex/dci/otn/controller/allocate/designer/site/NodeTp.java`
- Modify: `allocate/designer/src/main/java/net/flex/dci/otn/controller/allocate/designer/site/SiteNodeService.java:91-135`
- Modify: `allocate/designer/src/main/java/net/flex/dci/otn/controller/allocate/designer/site/SiteNodeService.java:720-729`

**Interfaces:**
- Consumes: the existing `RoutingType protectionPeerRole` passed from `SiteRepo.createSingleSiteNodeInfo` into `SiteNodeService.createSiteNode` and `createNeEquipAndTp`.
- Produces: `NodeTp.getProtectionPeerRole(): RoutingType` and a role-aware dual-FMUX branch in `createNeXcLink`.

- [ ] **Step 1: Write failing peer and main-node boundary tests**

Add the following imports to `SiteNodeServiceTest`:

```java
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.ProtectionBidir1To2;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.RoutingType;
```

Add two tests that invoke the real private node XC/link path. Use the existing
real `XCService`/`XCRepo` wiring pattern in this test class so the test does
not assert against a mock:

```java
@Test
void dedicatedFlex64OneToTwoPeerSkipsDualFmuxMainBuilder() throws Exception {
    SiteNodeService service = serviceWithRealXcService();
    NeInfo neInfo = bone20NeInfo();
    NodeTp peer = tilaOnlyNodeTp(neInfo, RoutingType.Slave);
    Method createNeXcLink = createNeXcLinkMethod();

    assertDoesNotThrow(() -> createNeXcLink.invoke(service, 0, peer, neInfo,
            ProtectionBidir1To2.class, false));
}

@Test
void roleNullFlex64NodeStillRequiresCompleteMainCardChain() throws Exception {
    SiteNodeService service = serviceWithRealXcService();
    NeInfo neInfo = bone20NeInfo();
    NodeTp incompleteMain = tilaOnlyNodeTp(neInfo, null);
    Method createNeXcLink = createNeXcLinkMethod();

    InvocationTargetException error = assertThrows(InvocationTargetException.class,
            () -> createNeXcLink.invoke(service, 0, incompleteMain, neInfo,
                    ProtectionBidir1To2.class, false));
    assertTrue(error.getCause() instanceof NeDesignerException);
    assertTrue(error.getCause().getMessage()
            .contains("Cannot find required PANEL occurrence 0 for Bone2.0 Flex64"));
}
```

Add these test-only helpers to `SiteNodeServiceTest`:

```java
private SiteNodeService serviceWithRealXcService() throws Exception {
    XCService xcService = new XCService();
    XCRepo repo = new XCRepo();
    Field jsonYangConverter = XCRepo.class.getDeclaredField("jsonYangConverter");
    jsonYangConverter.setAccessible(true);
    jsonYangConverter.set(repo, new JsonYangConverter());
    Field xcRepo = XCService.class.getDeclaredField("xcRepo");
    xcRepo.setAccessible(true);
    xcRepo.set(xcService, repo);

    SiteNodeService service = new SiteNodeService();
    Field serviceXc = SiteNodeService.class.getDeclaredField("xcService");
    serviceXc.setAccessible(true);
    serviceXc.set(service, xcService);
    return service;
}

private NeInfo bone20NeInfo() throws Exception {
    String resource = "COHERENT-CHASSIS2.0-ByteDance-OD-card.json";
    Ne ne = new ObjectMapper().readValue(
            getClass().getClassLoader().getResourceAsStream(resource), Ne.class);
    return new NeInfo(ne, resource);
}

private NodeTp tilaOnlyNodeTp(NeInfo neInfo, RoutingType protectionPeerRole) {
    CardTps tila = cardTps(neInfo.getCardByCardVendor("TILA"), 1,
            names("LINE_WEST", "LINE_EAST"),
            Collections.emptyMap(), Collections.emptyMap());
    return NodeTp.builder()
            .nodeId("Site-A#Ne-Peer")
            .nodeTpList(Collections.emptyList())
            .equipments(Collections.emptyList())
            .cardTps(new ArrayList<>(Arrays.asList(tila)))
            .slaveCardTps(new ArrayList<>())
            .thirdCardTps(new ArrayList<>())
            .resourceLayout(new ByteDance2OneToTwoResourceLayout(64))
            .protectionPeerRole(protectionPeerRole)
            .build();
}

private Method createNeXcLinkMethod() throws Exception {
    Method method = SiteNodeService.class.getDeclaredMethod("createNeXcLink",
            Integer.class, NodeTp.class, NeInfo.class, Class.class, Boolean.class);
    method.setAccessible(true);
    return method;
}
```

- [ ] **Step 2: Run the new tests and verify the first RED checkpoint**

Run:

```powershell
mvn -pl allocate/designer -am `
  "-Dtest=SiteNodeServiceTest#dedicatedFlex64OneToTwoPeerSkipsDualFmuxMainBuilder+roleNullFlex64NodeStillRequiresCompleteMainCardChain" `
  "-Dsurefire.failIfNoSpecifiedTests=false" test
```

Expected: test compilation fails because `NodeTpBuilder.protectionPeerRole` does
not exist. This proves the chosen role context has not yet reached `NodeTp`.

- [ ] **Step 3: Add and propagate the NodeTp role**

In `NodeTp`, add the nullable field beside `resourceLayout`:

```java
private RoutingType protectionPeerRole;
```

In the `NodeTp.builder()` return block in `createNeEquipAndTp`, add:

```java
.protectionPeerRole(protectionPeerRole)
```

- [ ] **Step 4: Re-run the new tests and verify the behavioral RED checkpoint**

Run the same Maven command from Step 2.

Expected:

- `roleNullFlex64NodeStillRequiresCompleteMainCardChain` passes;
- `dedicatedFlex64OneToTwoPeerSkipsDualFmuxMainBuilder` fails with an
  `InvocationTargetException` whose cause contains
  `Cannot find required PANEL occurrence 0 for Bone2.0 Flex64`.

- [ ] **Step 5: Implement the minimal role-aware dual-FMUX predicate**

In `createNeXcLink`, compute one local predicate before the main link section:

```java
boolean usesDualFmux64MainTopology = nodeTp.getProtectionPeerRole() == null
        && nodeTp.getResourceLayout().usesDualFmux64Topology();
```

Use this predicate for both existing dual-FMUX decisions:

```java
if (usesDualFmux64MainTopology) {
    protectedCardTps = createDualFmux64Links(nodeTp.getResourceLayout(), neInfo,
            nodeId, cardTpsList, reversed,
            nodeTp.getResourceLayout().getFmuxMainLinePort(isProtected),
            links, internalLinks, busyIds);
}

for (int i = 0; i < size; i++) {
    if (usesDualFmux64MainTopology) {
        continue;
    }
}
```

Only replace the two existing `usesDualFmux64Topology()` branch conditions;
leave the body of the generic card loop unchanged.

Do not inspect the card list to infer whether the node is a peer.

- [ ] **Step 6: Run the new tests and verify GREEN**

Run the Step 2 Maven command.

Expected: 2 tests run, 0 failures, 0 errors.

- [ ] **Step 7: Run the focused layout and repository regression suite**

Run:

```powershell
mvn -pl allocate/designer -am `
  "-Dtest=SiteNodeServiceTest,NodeTpTest,SiteResourceLayoutFactoryTest,SiteRepoTest" `
  "-Dsurefire.failIfNoSpecifiedTests=false" test
```

Expected: all selected tests pass with 0 failures and 0 errors.

- [ ] **Step 8: Review and commit the exact implementation scope**

Run:

```powershell
git diff --check
git diff -- allocate/designer/src/main/java/net/flex/dci/otn/controller/allocate/designer/site/NodeTp.java `
  allocate/designer/src/main/java/net/flex/dci/otn/controller/allocate/designer/site/SiteNodeService.java `
  allocate/designer/src/test/java/net/flex/dci/otn/controller/allocate/designer/site/SiteNodeServiceTest.java
```

Stage and commit only those three files:

```powershell
git add -- `
  allocate/designer/src/main/java/net/flex/dci/otn/controller/allocate/designer/site/NodeTp.java `
  allocate/designer/src/main/java/net/flex/dci/otn/controller/allocate/designer/site/SiteNodeService.java `
  allocate/designer/src/test/java/net/flex/dci/otn/controller/allocate/designer/site/SiteNodeServiceTest.java
git commit -m "fix: keep Flex64 OMSP 1:3 peers out of main topology"
```

### Task 2: Verify the module and close the DCI change loop

**Files:**
- Verify: `allocate/designer`
- Refresh: workspace contract, CodeGraph, project snapshot, and change history through `scripts/finish-dci-change.ps1`

**Interfaces:**
- Consumes: the role-aware `NodeTp` behavior from Task 1.
- Produces: fresh Maven evidence and a closed `.dci-agent/current-change.json` record.

- [ ] **Step 1: Run the complete alloc-designer test suite**

Run:

```powershell
mvn -pl allocate/designer -am test
```

Expected: reactor exits 0 and `alloc-designer` reports 0 failures and 0 errors.
If an unrelated baseline failure occurs, record the exact module, test, and
stack trace; do not report the full suite as passing.

- [ ] **Step 2: Perform final repository checks**

Run:

```powershell
git diff --check HEAD^ HEAD
git status --short
git show --stat --oneline HEAD
```

Expected: the implementation commit contains only the two production files and
one test file. Existing unrelated untracked paths remain untouched.

- [ ] **Step 3: Close the DCI workflow**

From `C:\Users\y3446\Documents\DCI`, run:

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\finish-dci-change.ps1 `
  -Summary "Preserve protectionPeerRole in NodeTp and skip the full dual-FMUX builder for Bone2.0 Flex64 OMSP 1:3 dedicated Slave and Third peer nodes" `
  -Verification "Focused peer/main boundary tests, selected designer regressions, complete alloc-designer suite, and git diff checks"
```

Expected: the active DCI change record is closed and the generated knowledge
artifacts refresh successfully. If CodeGraph remains unindexed, record that
limitation rather than claiming graph coverage.

- [ ] **Step 4: Report deployment boundary**

Report the exact commit and test counts. Mark 112 deployment and live recreation
of `OMSP1:3` / `OMSP1:3(2)` as `NOT RUN` unless the user separately authorizes
deployment and runtime validation.
