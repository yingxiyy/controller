# Restore TILA Class-Mode Contract Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Restore BONE2.0 TILA `class-mode` emission in allocate while preserving later frequency-model work.

**Architecture:** Reinstate the layout capability and resolution methods removed by `6aaeac337`, then apply the resolved value through `EquipmentsBuilder.setClassMode`. Reuse the previous tested behavior exactly: endpoint TILA is `OTA_WEST`, transit TILA is `ILA`, and all unrelated equipment receives no class-mode.

**Tech Stack:** Java 8, Maven, OpenDaylight generated YANG builders, JUnit 5

## Global Constraints

- Target repository and branch: `controller/bytedance`.
- Preserve local commits `0baf36919`, `1241ebe63`, and `c8d2f4d16`.
- Do not modify platform, telemetry, web, NMS, physical topology, routing, or frequency behavior.
- Preserve existing untracked files, especially `auth-service/auth-rest/bin/`.
- Do not commit or push unless the user explicitly requests it.

---

### Task 1: Restore The Regression Signal

**Files:**
- Modify: `allocate/designer/src/test/java/net/flex/dci/otn/controller/allocate/designer/site/SiteResourceLayoutFactoryTest.java`

**Interfaces:**
- Consumes: `SiteResourceLayout.resolveTilaClassMode(String, boolean, RoutingType)` and `supportsTilaClassMode()`.
- Produces: assertions for endpoint `OTA_WEST` and transit `ILA` behavior.

- [ ] **Step 1: Restore the three assertions removed by `6aaeac337`**

Restore the endpoint assertion in both BONE2.0 layout tests and the transit
assertion using `DefaultSiteResourceLayout`.

- [ ] **Step 2: Run the focused test and verify RED**

Run:

```powershell
mvn -pl allocate/designer '-Dtest=SiteResourceLayoutFactoryTest' test
```

Expected: test compilation fails because `resolveTilaClassMode` and
`supportsTilaClassMode` are absent.

### Task 2: Restore The Minimal Production Contract

**Files:**
- Modify: `allocate/designer/src/main/java/net/flex/dci/otn/controller/allocate/designer/site/ByteDance2DefaultResourceLayout.java`
- Modify: `allocate/designer/src/main/java/net/flex/dci/otn/controller/allocate/designer/site/ByteDance2FixedOneToTwoResourceLayout.java`
- Modify: `allocate/designer/src/main/java/net/flex/dci/otn/controller/allocate/designer/site/ByteDance2Flex64ResourceLayout.java`
- Modify: `allocate/designer/src/main/java/net/flex/dci/otn/controller/allocate/designer/site/ByteDance2FlexOneToOneResourceLayout.java`
- Modify: `allocate/designer/src/main/java/net/flex/dci/otn/controller/allocate/designer/site/ByteDance2OneToTwoResourceLayout.java`
- Modify: `allocate/designer/src/main/java/net/flex/dci/otn/controller/allocate/designer/site/DefaultSiteResourceLayout.java`
- Modify: `allocate/designer/src/main/java/net/flex/dci/otn/controller/allocate/designer/site/SiteNodeService.java`
- Modify: `allocate/designer/src/main/java/net/flex/dci/otn/controller/allocate/designer/site/SiteRepo.java`
- Modify: `allocate/designer/src/main/java/net/flex/dci/otn/controller/allocate/designer/site/SiteResourceLayout.java`

**Interfaces:**
- Consumes: generated `EquipmentsBuilder.setClassMode(String)` from platform YANG.
- Produces: `OTA_WEST` for BONE2.0 endpoint TILA and `ILA` for transit TILA.

- [ ] **Step 1: Restore the exact production hunks removed by `6aaeac337`**

Apply the inverse patch for the nine production files without reverting the
later manager/frequency commits.

- [ ] **Step 2: Run focused verification and verify GREEN**

Run:

```powershell
mvn -pl allocate/designer '-Dtest=SiteResourceLayoutFactoryTest' test
```

Expected: all selected tests pass with zero failures and errors.

- [ ] **Step 3: Run proportional compile and diff verification**

Run:

```powershell
mvn -pl allocate/designer -DskipTests compile
git diff --check
git status --short --branch
```

Expected: compile exits zero; diff check is clean; only the ten restored
class-mode files and task documentation are changed, with prior untracked files
preserved.
