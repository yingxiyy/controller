# Device Maintenance Download Rollback Defense Implementation Plan

> **For Codex:** REQUIRED SUB-SKILL: Use executing-plans to implement this plan task-by-task.

**Goal:** Prevent a device's post-rollback `COMPLETE` current-state snapshot from falsely completing a failed or still-running download task, while preserving valid correlated terminal notifications and existing behavior for other maintenance operations.

**Architecture:** Add one small, side-effect-free download evidence policy inside `device-maintenance`, and reuse persisted task state as the authority at every place that currently treats OP `download-state=COMPLETE` as proof. Notification completion/failure requires operation correlation; polling becomes fail-closed for download completion; workflow auto-progress and retry prechecks require controller-side completion evidence. No adapter, database schema, Kafka topology, Beijing data, or other repository is changed.

**Tech Stack:** Java 8, Spring Boot, JPA entities/repositories, Kafka listener, JUnit 5, Mockito, Maven.

**Global constraints:** Work only in `C:\Users\y3446\Documents\DCI\w\dm-download-rollback-defense` on `codex/dm-download-rollback-defense`, based on confirmed `bytedance`. Preserve the original controller worktree and its untracked BONE2 documents. Do not connect to, write, retry, deploy, restart, or otherwise mutate the Beijing environment. Write each regression first and observe the intended failure before production changes.

---

### Task 1: Introduce a deterministic download terminal-evidence policy

**Files:**
- Create: `device-maintenance/src/main/java/devicemaintenance/service/DownloadTerminalEvidencePolicy.java`
- Test: `device-maintenance/src/test/java/devicemaintenance/service/DownloadTerminalEvidencePolicyTest.java`

**Step 1: Write failing policy tests**

Cover these cases with a `RUNNING` download task whose `filePath` is `/upload/deviceSoftware/huawei/908.pkg` and whose `startedTime` is fixed:

```java
assertFalse(policy.validate(task, stateOnlyComplete).isAccepted());
assertEquals("missing-file-name", policy.validate(task, stateOnlyComplete).getReason());
assertTrue(policy.validate(task, matchingComplete).isAccepted());
assertEquals("file-name-mismatch", policy.validate(task, wrongFile).getReason());
assertEquals("stale-operation-time", policy.validate(task, staleTime).getReason());
assertEquals("malformed-operation-time", policy.validate(task, malformedTime).getReason());
```

Also reject a non-`RUNNING` task and a task without `startedTime`.

**Step 2: Run the new test and verify it fails**

Run:

```powershell
mvn '-Dtest=DownloadTerminalEvidencePolicyTest' '-Dmaven.gitcommitid.skip=true' test
```

Expected: compilation/test failure because the policy does not exist.

**Step 3: Implement the smallest policy**

Create a stateless Java 8 class with:

```java
public final class DownloadTerminalEvidencePolicy {
    public static Validation validate(DeviceTask task, Map<String, String> notification) { ... }

    public static boolean isLatestDownloadCompleted(List<DeviceTask> tasks) { ... }

    public static final class Validation {
        public boolean isAccepted() { ... }
        public String getReason() { ... }
    }
}
```

`validate` must require all of the following:

- task status is exactly `RUNNING`;
- task `startedTime` exists;
- notification contains `download.file-name` (fall back to `file-name` only for compatibility);
- the notification basename exactly matches the basename of `task.filePath`, after normalizing `\\` to `/`;
- notification contains `download.download-time` (fall back to `download-time`);
- the time parses using `DateTimeFormatter.ISO_OFFSET_DATE_TIME`;
- its instant is not older than `task.startedTime` in the system zone minus a fixed two-minute clock-skew allowance.

Return stable reason strings: `task-not-running`, `missing-task-start`, `missing-file-name`, `file-name-mismatch`, `missing-operation-time`, `malformed-operation-time`, and `stale-operation-time`.

`isLatestDownloadCompleted` must filter to `DOWNLOAD`, choose the newest task by `updatedTime` with `createdTime` as fallback, and return true only when that task is `COMPLETED`. Empty or null input returns false.

**Step 4: Run the policy tests and verify they pass**

Run the Task 1 command again. Expected: all policy tests pass.

**Step 5: Commit**

```powershell
git add device-maintenance/src/main/java/devicemaintenance/service/DownloadTerminalEvidencePolicy.java device-maintenance/src/test/java/devicemaintenance/service/DownloadTerminalEvidencePolicyTest.java
git commit -m "test: define download terminal evidence"
```

---

### Task 2: Defend the Kafka notification transition

**Files:**
- Modify: `device-maintenance/src/main/java/devicemaintenance/listener/SystemChangeNotificationListener.java`
- Modify: `device-maintenance/src/test/java/devicemaintenance/listener/SystemChangeNotificationListenerTest.java`

**Step 1: Add failing listener regressions**

Refactor the existing test setup with a helper that builds a `RUNNING` task and a notification payload. Add tests proving:

- state-only `COMPLETE` leaves the task `RUNNING` and does not save it;
- matching filename plus fresh operation time lets `COMPLETE` set `COMPLETED`;
- matching filename plus fresh operation time lets `FAIL` set `FAILED`;
- mismatched filename is ignored;
- operation time older than `startedTime - 2 minutes` is ignored.

Update `handleSystemChangeNotification_syncsBatchTargetVersionFromDownloadSoftwareVersion` to include matching `download.file-name`, a valid fresh `download.download-time`, task `filePath`, and task `startedTime`, so it represents strong completion evidence.

**Step 2: Run and verify the new tests fail on current behavior**

```powershell
mvn '-Dtest=SystemChangeNotificationListenerTest' '-Dmaven.gitcommitid.skip=true' test
```

Expected: state-only/mismatched/stale terminal messages are incorrectly applied by current code.

**Step 3: Apply the policy before terminal transitions**

In `updateTaskStatusForDownload`, for normalized `COMPLETE` and `FAIL`, call `DownloadTerminalEvidencePolicy.validate(task, notification)` before mutating task state. If rejected, log one structured warning containing task id, device id, state, and rejection reason, then return `false` without updating metadata, saving the task, changing the workflow, or scheduling a batch update.

Keep `DOWNLOADING` handling unchanged. Keep terminal-state monotonicity: already-terminal tasks remain unchanged.

**Step 4: Run the listener tests and verify they pass**

Run the Task 2 command again. Expected: all listener regressions pass.

**Step 5: Commit**

```powershell
git add device-maintenance/src/main/java/devicemaintenance/listener/SystemChangeNotificationListener.java device-maintenance/src/test/java/devicemaintenance/listener/SystemChangeNotificationListenerTest.java
git commit -m "fix: correlate download terminal notifications"
```

---

### Task 3: Make download polling fail-closed

**Files:**
- Modify: `device-maintenance/src/main/java/devicemaintenance/service/UpgradeTaskPollingService.java`
- Create: `device-maintenance/src/test/java/devicemaintenance/service/UpgradeTaskPollingServiceTest.java`

**Step 1: Add failing polling tests**

Make `determineTaskStatus` package-private so the service test can directly exercise the status mapping without timers. Instantiate the service with Mockito mocks and assert:

```java
assertNull(service.determineTaskStatus(task, "COMPLETE", DOWNLOAD, status));
assertEquals(FAILED, service.determineTaskStatus(task, "FAIL", DOWNLOAD, status));
assertEquals(COMPLETED, service.determineTaskStatus(task, "COMPLETE", BACKUP, status));
```

The third assertion prevents accidental behavior changes outside download.

**Step 2: Run and verify the regression fails**

```powershell
mvn '-Dtest=UpgradeTaskPollingServiceTest' '-Dmaven.gitcommitid.skip=true' test
```

Expected: current download `COMPLETE` maps to `COMPLETED`.

**Step 3: Separate DOWNLOAD from BACKUP mapping**

For `DOWNLOAD`, map `FAIL`/`FAILED` to `FAILED`, but return `null` for `COMPLETE` and log that a current-state snapshot cannot prove completion for the active operation. Leave `BACKUP`, `RESTORE`, `UPGRADE`, `ROLLBACK`, and `COMMIT` mappings unchanged.

**Step 4: Run the polling tests and verify they pass**

Run the Task 3 command again.

**Step 5: Commit**

```powershell
git add device-maintenance/src/main/java/devicemaintenance/service/UpgradeTaskPollingService.java device-maintenance/src/test/java/devicemaintenance/service/UpgradeTaskPollingServiceTest.java
git commit -m "fix: reject download completion from polling snapshot"
```

---

### Task 4: Require persisted completion before automatic workflow progress

**Files:**
- Modify: `device-maintenance/src/main/java/devicemaintenance/listener/SystemChangeNotificationListener.java`
- Modify: `device-maintenance/src/test/java/devicemaintenance/service/DownloadTerminalEvidencePolicyTest.java`
- Modify: `device-maintenance/src/test/java/devicemaintenance/listener/SystemChangeNotificationListenerTest.java`

**Step 1: Add failing gate tests**

Test `DownloadTerminalEvidencePolicy.isLatestDownloadCompleted` with:

- latest download task `FAILED`, older download task `COMPLETED` -> false;
- latest download task `RUNNING` -> false;
- latest download task `COMPLETED` -> true.

Add a listener-level automatic workflow test, invoking the smallest accessible helper or `findReadyWorkflows` through a package-private seam, proving OP `download-state=COMPLETE` plus a latest `RUNNING`/`FAILED` task does not make the workflow ready.

**Step 2: Run and verify the gate tests fail**

```powershell
mvn '-Dtest=DownloadTerminalEvidencePolicyTest,SystemChangeNotificationListenerTest' '-Dmaven.gitcommitid.skip=true' test
```

Expected: current OP-only gate returns ready.

**Step 3: Harden the DOWNLOAD auto-progress case**

In `isWorkflowStepConfirmedByOp`, compute:

```java
boolean downloadReady = "COMPLETE".equals(downloadState)
    && DownloadTerminalEvidencePolicy.isLatestDownloadCompleted(tasks);
```

Include both the OP state and persisted-completion result in the structured log. Do not alter other workflow steps.

**Step 4: Run and verify tests pass**

Run the Task 4 command again.

**Step 5: Commit**

```powershell
git add device-maintenance/src/main/java/devicemaintenance/listener/SystemChangeNotificationListener.java device-maintenance/src/test/java/devicemaintenance/service/DownloadTerminalEvidencePolicyTest.java device-maintenance/src/test/java/devicemaintenance/listener/SystemChangeNotificationListenerTest.java
git commit -m "fix: gate download workflow on persisted completion"
```

---

### Task 5: Ensure a failed download retry sends a real download RPC

**Files:**
- Modify: `device-maintenance/src/main/java/devicemaintenance/service/BatchUpgradeService.java`
- Modify: `device-maintenance/src/test/java/devicemaintenance/service/BatchUpgradeServiceTest.java`

**Step 1: Add the failing retry regression**

Build a failed `DOWNLOAD` task and failed workflow. Mock OP with `download-state=COMPLETE`, invoke `retryWorkflow`, and verify:

```java
verify(softwareDownloadService).executeScheduledTask(failedTask);
assertEquals(DeviceTask.TaskStatus.PENDING, failedTask.getStatus());
```

Also verify the failed task is not silently synchronized to `COMPLETED`. Stub the existing workflow/status/batch collaborators only as far as required by the real retry path.

**Step 2: Run and verify it fails**

```powershell
mvn '-Dtest=BatchUpgradeServiceTest' '-Dmaven.gitcommitid.skip=true' test
```

Expected: current code returns early after OP `COMPLETE`, marks the task completed, and does not invoke download execution.

**Step 3: Disable OP-only completion synchronization for DOWNLOAD**

In `isFailedStepAlreadyCompletedInOp`, make the `DOWNLOAD` case always return false after logging the observed OP state and that it is insufficient proof because rollback restores the normal snapshot. Keep BACKUP/UPGRADE/COMMIT prechecks unchanged.

**Step 4: Run and verify the retry tests pass**

Run the Task 5 command again.

**Step 5: Commit**

```powershell
git add device-maintenance/src/main/java/devicemaintenance/service/BatchUpgradeService.java device-maintenance/src/test/java/devicemaintenance/service/BatchUpgradeServiceTest.java
git commit -m "fix: execute failed download retries despite OP snapshot"
```

---

### Task 6: Verify the complete defensive boundary and close the DCI change

**Files:**
- Modify if needed: `docs/superpowers/specs/2026-08-26-device-maintenance-download-rollback-defense-design.md`
- Modify if needed: `DCI-Knowledge/` notes selected by the DCI finish script
- Review: all files changed from `bytedance`

**Step 1: Run focused regressions together**

```powershell
mvn '-Dtest=DownloadTerminalEvidencePolicyTest,SystemChangeNotificationListenerTest,UpgradeTaskPollingServiceTest,BatchUpgradeServiceTest' '-Dmaven.gitcommitid.skip=true' test
```

Expected: all focused tests pass with no failure or error.

**Step 2: Run the whole device-maintenance test suite**

```powershell
mvn '-Dmaven.gitcommitid.skip=true' test
```

Expected: `BUILD SUCCESS`. Record test counts and any baseline-only warnings.

**Step 3: Inspect scope and diff**

```powershell
git status --short
git diff bytedance...HEAD --stat
git diff bytedance...HEAD --check
git log --oneline --decorate bytedance..HEAD
```

Confirm there are no adapter files, Beijing artifacts/data, build outputs, secrets, or unrelated BONE2 files in the change.

**Step 4: Refresh local change intelligence and close the recorded change**

CodeGraph is unavailable in this environment, so document that boundary and use the repository's finish workflow to refresh the available contracts/snapshots/knowledge:

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\finish-dci-change.ps1
```

Run from `C:\Users\y3446\Documents\DCI`. Do not let the finish workflow stage or commit unrelated files; inspect its output and resulting status.

**Step 5: Final handoff**

Report:

- the four defended paths (notification, polling, auto-progress, retry);
- exact tests and results;
- branch/worktree and commits;
- CodeGraph unavailable boundary;
- explicit confirmation that no Beijing data/environment and no `eml-adapter` files were changed;
- quarantined auth-rest build output remains recoverable at `C:\Users\y3446\Documents\DCI\.codex-tmp\quarantine\controller-auth-rest-bin-20260826`.
