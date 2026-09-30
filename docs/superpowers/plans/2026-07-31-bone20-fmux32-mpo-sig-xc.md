# Bone2.0 FMUX32 MPO-SIG XC Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Generate the missing fixed FMUX32 `MPO1..MPO4 -> SIG` cross-connection so physical topology, persisted SiteLink routes, and frontend flowcharts are continuous.

**Architecture:** Define the fixed passive XC in the Bone2.0 card model, then narrow the Flex32 special-case transformation so it rewrites only APS XCs. Reuse the existing SiteLink MPO aggregation to serialize the four physical MPO sources as one virtual `MPO -> SIG` route XC.

**Tech Stack:** Java 8, Jackson card-model JSON, JUnit 5, Maven, generated YANG topology classes.

## Global Constraints

- Target repository and branch: `controller/bytedance`.
- Keep existing equipment, TP, link, APS, and protection-route identifiers.
- Do not modify frontend code.
- Do not migrate existing persisted SiteLinks.
- Do not commit unless the user explicitly requests a commit.

---

### Task 1: Specify The Fixed FMUX32 XC

**Files:**
- Modify: `allocate/designer/src/test/java/net/flex/dci/otn/controller/allocate/designer/model/NeInfoTest.java`
- Modify: `allocate/designer/src/main/resources/COHERENT-CHASSIS2.0-ByteDance-OD-card.json:532-549`

**Interfaces:**
- Consumes: `NeInfo.getCardByCardVendor(String)` and `Card.getCrossConnections()`.
- Produces: one fixed OCH model XC from `MPO?,1,4,1` to `SIG`, in addition to the existing APS XC.

- [ ] **Step 1: Add a failing card-model assertion**

Extend `bone20OdCardHasConsistentIraAndMuxPanelLinks()` to select the FMUX XC whose `from.port` is `MPO?,1,4,1` and assert:

```java
CrossConnection mpoSig = neInfo.getCardByCardVendor("FMUX_32").getCrossConnections().stream()
        .filter(xc -> "MPO?,1,4,1".equals(xc.getFrom().getPort()))
        .findFirst()
        .orElseThrow(() -> new AssertionError("Missing FMUX32 MPO-SIG XC"));
assertEquals("SIG", mpoSig.getTo().getPort());
assertEquals("OCH", mpoSig.getType());
assertTrue(mpoSig.getInitiated());
assertTrue(mpoSig.getIsFixed());
assertTrue(mpoSig.getBiDirection());
assertEquals(Integer.valueOf(1), mpoSig.getMultiple());
```

- [ ] **Step 2: Run the focused test and confirm RED**

Run:

```powershell
mvn -pl allocate/designer -Dtest=NeInfoTest#bone20OdCardHasConsistentIraAndMuxPanelLinks test
```

Expected: FAIL with `Missing FMUX32 MPO-SIG XC`.

- [ ] **Step 3: Add the minimal JSON model entry**

Insert before the existing APS object in FMUX32 `crossConnections`:

```json
{
  "initiated": true,
  "isFixed": true,
  "type": "OCH",
  "biDirection": true,
  "multiple": 1,
  "description": "FMUX-1-<slot>-1",
  "from": {
    "port": "MPO?,1,4,1"
  },
  "to": {
    "port": "SIG"
  }
}
```

- [ ] **Step 4: Run the focused test and confirm GREEN**

Run the command from Step 2. Expected: PASS.

### Task 2: Preserve Non-APS FMUX XCs In Flex32

**Files:**
- Modify: `allocate/designer/src/test/java/net/flex/dci/otn/controller/allocate/designer/site/SiteNodeServiceTest.java`
- Modify: `allocate/designer/src/main/java/net/flex/dci/otn/controller/allocate/designer/site/SiteNodeService.java:496-531`

**Interfaces:**
- Consumes: card-model `CrossConnection` objects.
- Produces: package-private `adaptFmux32ProtectionXc(CrossConnection)`; ordinary XCs are returned unchanged, APS XCs are copied with `COM1 -> SIGA,SIGB` endpoints.

- [ ] **Step 1: Add failing adaptation tests**

Add one test with an ordinary XC and one with an APS object:

```java
@Test
void rewritesOnlyFmux32ApsCrossConnection() {
    SiteNodeService service = new SiteNodeService();
    CrossConnection ordinary = crossConnection("MPO?,1,4,1", "SIG", null);
    CrossConnection aps = crossConnection("SIGA,SIGB", "COM1,COM2", new Aps());

    assertSame(ordinary, service.adaptFmux32ProtectionXc(ordinary));
    CrossConnection adaptedAps = service.adaptFmux32ProtectionXc(aps);
    assertEquals("COM1", adaptedAps.getFrom().getPort());
    assertEquals("SIGA,SIGB", adaptedAps.getTo().getPort());
    assertNotNull(adaptedAps.getAps());
}
```

Create the test helper with `CrossConnectionPoint` instances and assign the supplied ports and APS object. Import `net.flex.dci.otn.controller.allocate.ne.Aps`, `CrossConnection`, and `CrossConnectionPoint`.

- [ ] **Step 2: Run the focused test and confirm RED**

Run:

```powershell
mvn -pl allocate/designer -Dtest=SiteNodeServiceTest#rewritesOnlyFmux32ApsCrossConnection test
```

Expected: test compilation fails because `adaptFmux32ProtectionXc` does not exist.

- [ ] **Step 3: Implement APS-only adaptation**

Replace unconditional copying inside `createFmux32ProtectionXcs` with:

```java
CrossConnection fmux32Xc = adaptFmux32ProtectionXc(crossConnection);
xcs.addAll(xcService.createXCs(nodeId, totalPortTp, fmux32Xc, isProtected));
```

Add:

```java
CrossConnection adaptFmux32ProtectionXc(CrossConnection crossConnection) {
    if (crossConnection.getAps() == null) {
        return crossConnection;
    }
    return copyCrossConnectionWithPorts(crossConnection, "COM1", "SIGA,SIGB");
}
```

- [ ] **Step 4: Run both designer tests and confirm GREEN**

Run:

```powershell
mvn -pl allocate/designer -Dtest=NeInfoTest#bone20OdCardHasConsistentIraAndMuxPanelLinks,SiteNodeServiceTest#rewritesOnlyFmux32ApsCrossConnection test
```

Expected: both tests pass.

### Task 3: Verify Physical Fan-In And SiteLink Aggregation

**Files:**
- Modify: `allocate/designer/src/test/java/net/flex/dci/otn/controller/allocate/designer/site/SiteNodeServiceTest.java`
- Modify: `allocate/manager/src/test/java/net/flex/dci/otn/controller/allocate/link/common/RouteTest.java`
- No production change expected.

**Interfaces:**
- Consumes: `XCService.createXCs(...)` and `Route.getRouteXC(...)` behavior.
- Produces: regression evidence that physical MPO1-4 fan-in becomes one virtual SiteLink MPO source without duplicate XC IDs.

- [ ] **Step 1: Test physical XC expansion**

Add this test, using the `crossConnection` helper from Task 2 and reflection only to inject the existing private dependency:

```java
@Test
void createsOnePhysicalFmuxMpoSigFanIn() throws Exception {
    XCService xcService = new XCService();
    Field xcRepo = XCService.class.getDeclaredField("xcRepo");
    xcRepo.setAccessible(true);
    xcRepo.set(xcService, new XCRepo());

    CrossConnection definition = crossConnection("MPO?,1,4,1", "SIG", null);
    definition.setType("OCH");
    definition.setInitiated(true);
    definition.setIsFixed(true);
    definition.setBiDirection(true);
    definition.setMultiple(1);
    definition.setDescription("FMUX-1-<slot>-1");

    Map<String, String> ports = new LinkedHashMap<>();
    ports.put("MPO1", "Site-A#Ne-A#LINECARD-1-3#PORT-1-3-MPO1");
    ports.put("MPO2", "Site-A#Ne-A#LINECARD-1-3#PORT-1-3-MPO2");
    ports.put("MPO3", "Site-A#Ne-A#LINECARD-1-3#PORT-1-3-MPO3");
    ports.put("MPO4", "Site-A#Ne-A#LINECARD-1-3#PORT-1-3-MPO4");
    ports.put("SIG", "Site-A#Ne-A#LINECARD-1-3#PORT-1-3-SIG");

    List<CrossConnections> result = xcService.createXCs("Site-A#Ne-A", ports,
            definition, true);
    assertEquals(1, result.size());
    assertEquals(4, result.get(0).getSourceTp().size());
    assertEquals(1, result.get(0).getDestinationTp().size());
    assertEquals("Site-A#Ne-A#LINECARD-1-3#PORT-1-3-SIG",
            result.get(0).getDestinationTp().get(0).getTpRef().getValue());
}
```

Import `java.lang.reflect.Field`, `XCRepo`, and the physical YANG `CrossConnections` type.

- [ ] **Step 2: Run the physical expansion test**

Run:

```powershell
mvn -pl allocate/designer -Dtest=SiteNodeServiceTest test
```

Expected: PASS after Tasks 1-2; a failure identifies an incorrect `multiple` or endpoint expression.

- [ ] **Step 3: Test route-level MPO collapse**

In `RouteTest`, add a reflection-based contract test without widening production visibility:

```java
@Test
@SuppressWarnings("unchecked")
void collapsesFmuxMpoSigFanInForSiteLink() throws Exception {
    String prefix = "Site-A#Ne-A#LINECARD-1-3#PORT-1-3-";
    CrossConnections physical = new CrossConnectionsBuilder()
            .setCrossConnectionId(new Uri("XC-" + prefix + "MPO1-" + prefix + "MPO2-"
                    + prefix + "MPO3-" + prefix + "MPO4-" + prefix + "SIG"))
            .setSourceTp(Arrays.asList(
                    source(prefix + "MPO1"), source(prefix + "MPO2"),
                    source(prefix + "MPO3"), source(prefix + "MPO4")))
            .setDestinationTp(Arrays.asList(destination(prefix + "SIG")))
            .build();

    Route route = new Route(null, RouteType.SiteLink);
    Field aggregate = Route.class.getDeclaredField("hasMpoAggregatingCard");
    aggregate.setAccessible(true);
    aggregate.set(route, true);
    Method convert = Route.class.getDeclaredMethod("getRouteXC", List.class);
    convert.setAccessible(true);

    List<org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.types.rev180515
            .cross.connection.route.sequence.CrossConnections> result =
            (List<org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.types.rev180515
                    .cross.connection.route.sequence.CrossConnections>)
                    convert.invoke(route, Arrays.asList(physical));

    assertEquals(1, result.size());
    assertEquals(prefix + "MPO", result.get(0).getSourceTp().get(0).getTpRef().getValue());
    assertEquals(prefix + "SIG", result.get(0).getDestinationTp().get(0).getTpRef().getValue());
}

private SourceTp source(String id) {
    return new SourceTpBuilder().setTpRef(new TpId(id)).build();
}

private DestinationTp destination(String id) {
    return new DestinationTpBuilder().setTpRef(new TpId(id)).build();
}
```

Import `java.lang.reflect.Field`, `java.lang.reflect.Method`, `Uri`, `TpId`, the physical `CrossConnections`/builder, and physical source/destination TP builders.

- [ ] **Step 4: Run the manager regression test**

Run:

```powershell
mvn -pl allocate/manager -Dtest=RouteTest test
```

Expected: all `RouteTest` cases pass.

### Task 4: Proportional Verification And Knowledge Closure

**Files:**
- Update: `DCI-Knowledge/04-Operations/SiteLink-Mongo-Inspection.md`
- Generated by closure scripts: contract map, CodeGraph index, project snapshot, change history.

**Interfaces:**
- Consumes: all changes from Tasks 1-3.
- Produces: verified controller change and durable troubleshooting guidance.

- [ ] **Step 1: Run focused tests together**

```powershell
mvn -pl allocate/designer,allocate/manager -am `
  -Dtest=NeInfoTest,SiteNodeServiceTest,RouteTest `
  -Dsurefire.failIfNoSpecifiedTests=false test
```

Expected: all selected tests pass with zero failures/errors.

- [ ] **Step 2: Compile affected modules with Java 8**

```powershell
mvn -pl allocate/designer,allocate/manager -am `
  -DskipTests -Dcheckstyle.skip compile
```

Expected: BUILD SUCCESS. If unrelated generated-YANG baseline failures occur before affected sources compile, record the exact blocker and run the narrowest direct module compilation available.

- [ ] **Step 3: Inspect the final diff**

Run `git status --short`, `git diff --check`, and `git diff --` for the four intended source/test files and design/plan docs. Confirm the pre-existing untracked `auth-service/auth-rest/bin/` remains untouched.

- [ ] **Step 4: Update durable knowledge**

Document that FMUX32 requires the fixed chain `MPO -> SIG -> COM1 -> SIGA/SIGB`, that the physical XC has four MPO source TPs, and that SiteLink serializes one virtual MPO source.

- [ ] **Step 5: Refresh CodeGraph and close the change**

Run `scripts/finish-dci-change.ps1` with the implementation summary and exact verification output. Do not commit or push unless explicitly requested.
