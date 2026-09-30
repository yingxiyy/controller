# Bone2.0 CHASSIS2.0 Frequency Range Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Generate Bone2.0 `CHASSIS2.0` 150 GHz SiteLink channels from `191325000` through `196125000` without changing legacy ByteDance frequency behavior.

**Architecture:** Add a distinct `NeYangModel.Chassis20` model with an explicit C-band range in platform common utilities. Resolve Bone2.0 SiteLinks to that model in controller allocation, persist the model on new SiteLinks, and recognize the product type on existing SiteLinks for model-aware calculations.

**Tech Stack:** Java 8, Maven 3.9.9, JUnit Jupiter 5.10.2, OpenDaylight generated YANG model classes.

## Global Constraints

- Work only on the confirmed `bytedance` branch.
- Preserve all unrelated tracked and untracked files in platform, controller, telemetry, and web.
- Chassis2.0 C-band lower frequency is exactly `191325000`; upper frequency is exactly `196125000`.
- A 150 GHz Chassis2.0 grid contains exactly 32 adjacent intervals and centers `191400000 + n * 150000` for `n=0..31`.
- Do not change legacy ByteDance C/C+L ranges, the L-band range, `order-id`, resource JSON, YANG, web, NMS, or MongoDB data.
- Keep Java 8 compatibility and existing package/style conventions.

---

## File Structure

- `platform/common/common-util/src/main/java/net/flex/dci/otc/common/util/NeYangModel.java`: defines the new model identity and aliases.
- `platform/common/common-util/src/main/java/net/flex/dci/otc/common/util/MuxCardPortFormatting.java`: keeps Chassis2.0 mux-port formatting compatible with ByteDance.
- `platform/common/common-util/src/main/java/net/flex/dci/otc/common/util/frequency/Constant.java`: owns Chassis2.0 band boundaries.
- `platform/common/common-util/src/main/java/net/flex/dci/otc/common/util/frequency/FrequencyAvailable.java`: selects the model range and generates intervals.
- `platform/common/common-util/src/test/java/net/flex/dci/otc/common/util/frequency/FrequencyAvailableTest.java`: verifies the new frequency contract and legacy compatibility.
- `controller/allocate/manager/src/main/java/net/flex/dci/otn/controller/allocate/link/site/SiteLinkCreator.java`: resolves the per-SiteLink frequency model.
- `controller/allocate/manager/src/test/java/net/flex/dci/otn/controller/allocate/link/site/SiteLinkCreatorExternalTest.java`: verifies Bone2.0 model selection.

### Task 1: Add the Chassis2.0 platform frequency model

**Files:**
- Modify: `../platform/common/common-util/src/main/java/net/flex/dci/otc/common/util/NeYangModel.java`
- Modify: `../platform/common/common-util/src/main/java/net/flex/dci/otc/common/util/MuxCardPortFormatting.java`
- Modify: `../platform/common/common-util/src/main/java/net/flex/dci/otc/common/util/frequency/Constant.java`
- Modify: `../platform/common/common-util/src/main/java/net/flex/dci/otc/common/util/frequency/FrequencyAvailable.java`
- Create: `../platform/common/common-util/src/test/java/net/flex/dci/otc/common/util/frequency/FrequencyAvailableTest.java`

**Interfaces:**
- Consumes: existing `FrequencyAvailable.getInitializedAvailableList(NeYangModel, WDM_Band, GridType)`.
- Produces: `NeYangModel.Chassis20`, `Constant.MaxLowerFrequency.BONE20_C`, `Constant.MaxUpperFrequency.BONE20_C`, and package-private `FrequencyAvailable.resolveYangModel(String, NeYangModel)`.

- [ ] **Step 1: Write the failing Chassis2.0 frequency tests**

Create `FrequencyAvailableTest` with these assertions:

```java
package net.flex.dci.otc.common.util.frequency;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.math.BigInteger;
import java.util.List;
import net.flex.dci.otc.common.util.NeYangModel;
import org.junit.jupiter.api.Test;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.GridType;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.site.link.attributes.site.Available;

class FrequencyAvailableTest {

    @Test
    void chassis20Fixed150UsesHardwareFrequencyPlan() {
        List<Available> frequencies = FrequencyAvailable.getInitializedAvailableList(
                NeYangModel.Chassis20, WDM_Band.C, GridType._150);

        assertEquals(32, frequencies.size());
        assertRange(frequencies.get(0), "191325000", "191475000");
        assertRange(frequencies.get(31), "195975000", "196125000");
        assertEquals(new BigInteger("191400000"), center(frequencies.get(0)));
        assertEquals(new BigInteger("196050000"), center(frequencies.get(31)));

        for (int i = 1; i < frequencies.size(); i++) {
            assertEquals(frequencies.get(i - 1).getUpperFrequency().getValue(),
                    frequencies.get(i).getLowerFrequency().getValue());
        }
    }

    @Test
    void legacyByteDanceFixed150KeepsExistingLowerBoundary() {
        List<Available> frequencies = FrequencyAvailable.getInitializedAvailableList(
                NeYangModel.ByteDance, WDM_Band.C, GridType._150);

        assertEquals(new BigInteger("191250000"),
                frequencies.get(0).getLowerFrequency().getValue());
    }

    @Test
    void existingChassis20SiteLinkOverridesLegacyStoredModel() {
        assertEquals(NeYangModel.Chassis20,
                FrequencyAvailable.resolveYangModel("CHASSIS2.0", NeYangModel.ByteDance));
    }

    private static void assertRange(Available available, String lower, String upper) {
        assertEquals(new BigInteger(lower), available.getLowerFrequency().getValue());
        assertEquals(new BigInteger(upper), available.getUpperFrequency().getValue());
    }

    private static BigInteger center(Available available) {
        return available.getLowerFrequency().getValue()
                .add(available.getUpperFrequency().getValue())
                .divide(BigInteger.valueOf(2));
    }
}
```

- [ ] **Step 2: Run the focused platform test and confirm the missing enum failure**

Run from `C:\Users\y3446\Documents\DCI\platform`:

```powershell
mvn -pl common/common-util -am -Dtest=FrequencyAvailableTest -Dsurefire.failIfNoSpecifiedTests=false test
```

Expected: compilation fails because `NeYangModel.Chassis20` does not exist.

- [ ] **Step 3: Add the model identity and hardware constants**

Add this enum entry to `NeYangModel`, preserving the existing ByteDance port contract:

```java
Chassis20("Chassis2.0") {
    @Override
    public String muxPortFormatter() {
        return "M%sD%s";
    }

    @Override
    public String muxPortMatchingRegex() {
        return "M\\d+D\\d+";
    }

    @Override
    public String muxChannelIdKeyword() {
        return "D";
    }
}
```

In `NeYangModel.getModel(Physical)`, map case-insensitive `Chassis2.0` and `ByteDance2.0` property values to `Chassis20`.

Add these constants to the corresponding nested classes in `Constant`:

```java
public static final String BONE20_C = "196125000";
```

```java
public static final String BONE20_C = "191325000";
```

- [ ] **Step 4: Implement the Chassis2.0 generation case and compatibility paths**

In `FrequencyAvailable.getAvailablesForC`, add:

```java
case Chassis20:
    lower = new BigInteger(Constant.MaxLowerFrequency.BONE20_C);
    higher = new BigInteger(Constant.MaxUpperFrequency.BONE20_C);
    break;
```

Add this package-private resolver and call it from `extractYangModel` after parsing the stored `yang-model` property:

```java
static NeYangModel resolveYangModel(String productType, NeYangModel storedModel) {
    return "CHASSIS2.0".equalsIgnoreCase(productType)
            ? NeYangModel.Chassis20
            : storedModel;
}
```

This ensures existing Bone2.0 SiteLinks override a legacy stored `ByteDance` model without changing any persisted record. Extend `inBandC` to use the Bone2.0 constants for `Chassis20`.

In all three switches in `MuxCardPortFormatting`, let `Chassis20` fall through with `ByteDance` so it returns `M%sD%s`, `M\\d+D\\d+`, and `D` respectively.

- [ ] **Step 5: Run the focused platform tests and inspect the diff**

Run:

```powershell
mvn -pl common/common-util -am -Dtest=FrequencyAvailableTest -Dsurefire.failIfNoSpecifiedTests=false test
git diff --check
git diff -- common/common-util/src/main/java/net/flex/dci/otc/common/util/NeYangModel.java common/common-util/src/main/java/net/flex/dci/otc/common/util/MuxCardPortFormatting.java common/common-util/src/main/java/net/flex/dci/otc/common/util/frequency/Constant.java common/common-util/src/main/java/net/flex/dci/otc/common/util/frequency/FrequencyAvailable.java common/common-util/src/test/java/net/flex/dci/otc/common/util/frequency/FrequencyAvailableTest.java
```

Expected: `FrequencyAvailableTest` passes; only the five requested platform files appear.

- [ ] **Step 6: Install the changed platform artifact for controller compilation**

```powershell
mvn -pl common/common-util -am -DskipTests install
```

Expected: the updated `common-util` artifact is installed in the local Maven repository.

- [ ] **Step 7: Commit the platform change**

```powershell
git add -- common/common-util/src/main/java/net/flex/dci/otc/common/util/NeYangModel.java common/common-util/src/main/java/net/flex/dci/otc/common/util/MuxCardPortFormatting.java common/common-util/src/main/java/net/flex/dci/otc/common/util/frequency/Constant.java common/common-util/src/main/java/net/flex/dci/otc/common/util/frequency/FrequencyAvailable.java common/common-util/src/test/java/net/flex/dci/otc/common/util/frequency/FrequencyAvailableTest.java
git diff --cached --check
git commit -m "fix: align Bone2.0 fixed-grid frequencies"
```

### Task 2: Select Chassis2.0 during SiteLink creation

**Files:**
- Modify: `allocate/manager/src/main/java/net/flex/dci/otn/controller/allocate/link/site/SiteLinkCreator.java`
- Modify: `allocate/manager/src/test/java/net/flex/dci/otn/controller/allocate/link/site/SiteLinkCreatorExternalTest.java`

**Interfaces:**
- Consumes: `NeYangModel.Chassis20` and `ProductTypeResolver.isBone20ProductType(String, String)`.
- Produces: package-private `SiteLinkCreator.resolveSiteLinkYangModel(NeYangModel, String, String)` returning the per-SiteLink model.

- [ ] **Step 1: Write the failing controller model-selection tests**

Append these tests to `SiteLinkCreatorExternalTest`:

```java
@Test
void bone20UsesChassis20FrequencyModel() {
    assertEquals(NeYangModel.Chassis20,
            SiteLinkCreator.resolveSiteLinkYangModel(
                    NeYangModel.ByteDance, "COHERENT", "CHASSIS2.0"));
}

@Test
void legacyProductKeepsConfiguredFrequencyModel() {
    assertEquals(NeYangModel.ByteDance,
            SiteLinkCreator.resolveSiteLinkYangModel(
                    NeYangModel.ByteDance, "COHERENT", "CHASSIS"));
}
```

Add imports for `assertEquals` and `NeYangModel`.

- [ ] **Step 2: Run the focused controller test and confirm the missing method failure**

Run from `C:\Users\y3446\Documents\DCI\controller` after installing the changed platform artifact:

```powershell
mvn -pl allocate/manager -am -Dtest=SiteLinkCreatorExternalTest -Dsurefire.failIfNoSpecifiedTests=false -Dcheckstyle.skip -Dmaven.javadoc.skip=true test
```

Expected: compilation fails because `resolveSiteLinkYangModel` does not exist.

- [ ] **Step 3: Implement per-SiteLink model selection**

Add this package-private helper to `SiteLinkCreator`:

```java
static NeYangModel resolveSiteLinkYangModel(NeYangModel configuredModel,
        String vendorName, String productType) {
    return ProductTypeResolver.isBone20ProductType(vendorName, productType)
            ? NeYangModel.Chassis20
            : configuredModel;
}
```

After `CreateSiteLinkParam` parses the input and before a creation method builds the SiteLink, assign:

```java
yangModel = resolveSiteLinkYangModel(yangModel,
        param.getVendorName(), param.getVendorType());
```

This makes `getSiteLinkProperties()` persist `yang-model=Chassis20` and makes the existing call to `FrequencyAvailable.getInitializedAvailableList` use the new case.

- [ ] **Step 4: Run focused controller and platform tests**

Run:

```powershell
mvn -pl allocate/manager -am -Dtest=SiteLinkCreatorExternalTest -Dsurefire.failIfNoSpecifiedTests=false -Dcheckstyle.skip -Dmaven.javadoc.skip=true test
git diff --check
```

Expected: the focused controller tests pass and no unrelated tracked file is changed.

- [ ] **Step 5: Commit the controller change**

```powershell
git add -- allocate/manager/src/main/java/net/flex/dci/otn/controller/allocate/link/site/SiteLinkCreator.java allocate/manager/src/test/java/net/flex/dci/otn/controller/allocate/link/site/SiteLinkCreatorExternalTest.java docs/superpowers/plans/2026-08-03-bone20-chassis20-frequency.md
git diff --cached --check
git commit -m "fix: select Bone2.0 frequency model"
```

### Task 3: Verify the cross-repository contract and close the DCI change

**Files:**
- Verify only: platform and controller changed files from Tasks 1 and 2.
- Generated by workflow when applicable: `.dci-agent` state and `DCI-Knowledge` summaries.

**Interfaces:**
- Consumes: installed platform `common-util` artifact and controller allocation modules.
- Produces: verified 32-channel Bone2.0 frequency behavior and refreshed DCI change records.

- [ ] **Step 1: Install the focused platform module for controller consumers**

Run from `C:\Users\y3446\Documents\DCI\platform`:

```powershell
mvn -pl common/common-util -am -DskipTests install
```

Expected: the updated `common-util` artifact installs successfully.

- [ ] **Step 2: Run proportional controller verification**

Run from `C:\Users\y3446\Documents\DCI\controller`:

```powershell
mvn -pl allocate/manager -am -Dtest=SiteLinkCreatorExternalTest -Dsurefire.failIfNoSpecifiedTests=false -Dcheckstyle.skip -Dmaven.javadoc.skip=true test
```

If an existing generated-YANG baseline failure prevents the full reactor, record it separately and retain the successful focused module evidence.

- [ ] **Step 3: Verify exact Git scope in every repository**

Run from `C:\Users\y3446\Documents\DCI`:

```powershell
git -C platform status --short
git -C platform show --stat --oneline HEAD
git -C controller status --short
git -C controller show --stat --oneline HEAD
git -C telemetry status --short
```

Expected: telemetry still contains only its pre-existing `.gitignore` change; controller still preserves unrelated untracked files; platform and controller commits contain only the requested files.

- [ ] **Step 4: Refresh DCI contracts, CodeGraph, knowledge, and change history**

Run from `C:\Users\y3446\Documents\DCI`:

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\finish-dci-change.ps1 -Summary "Add a Chassis2.0 frequency model so Bone2.0 fixed 150GHz SiteLinks generate 32 channels from 191325000 to 196125000 without changing legacy ByteDance ranges." -Verification "FrequencyAvailableTest and SiteLinkCreatorExternalTest focused checks; platform common-util install; exact Git scope inspection."
```

Expected: `.dci-agent/current-change.json` is cleared and the generated contract/knowledge refresh completes or reports a baseline limitation explicitly.

