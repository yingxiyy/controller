# Device Maintenance Download Rollback Defense Design

## Problem

Huawei DC908 reports an active download through `update/DOWNLOADING`. When the
SFTP attempt fails, the device rolls its operational state back to its normal
value. That normal value is exposed as `download-state=COMPLETE`. The adapter
can therefore publish a later, state-only `system-change` message containing
`download.download-state=COMPLETE`, even though no second download occurred.

`device-maintenance` currently treats every `COMPLETE` for the latest
incomplete download task as success. The same assumption also exists in its
long-running-task polling fallback, automatic workflow readiness check, and
failed-step retry precheck. These paths can turn a rollback snapshot into a
successful download and can even skip a requested retry.

## Scope

Change only `controller/device-maintenance` on target branch `bytedance`.
Do not modify `eml-adapter`, device YANG, Kafka topics, Beijing data, deployed
services, or unrelated controller modules.

The device-side contract is expected to emit terminal operation results as
`update FAIL` or `update COMPLETE` before any rollback cleanup. Terminal update
messages must include `download.file-name` and `download.download-time`.

## Design

### 1. Distinguish terminal operation evidence from a current-state snapshot

Introduce a small download-result evidence policy used by the notification
listener. A `COMPLETE` notification is strong enough to finish a task only
when all of the following are true:

- the matched task is currently `RUNNING`;
- `download.file-name` is present and its basename matches the task package;
- `download.download-time` is present and parseable;
- the task has a `startedTime`; and
- the device operation time is not older than `startedTime - 2 minutes`, which
  is the fixed clock-skew tolerance for this comparison.

A state-only `COMPLETE` is weak snapshot evidence. It is logged and ignored;
the task remains `RUNNING`.

Apply the same correlation rules to `FAIL`. A correlated `update FAIL` moves
the task to `FAILED`. Once failed, the existing repository query no longer
matches it as an incomplete task, so later rollback snapshots cannot overwrite
the terminal result.

### 2. Do not manufacture download success from polling

`UpgradeTaskPollingService` must not convert a download snapshot containing
`COMPLETE` into `DeviceTask.COMPLETED`. A current snapshot cannot distinguish a
real successful transfer from the device's rollback-to-normal state. Polling
may still recognize an explicit current `FAIL`; otherwise it keeps the task
running until a terminal notification arrives or the existing maximum wait
marks it failed.

This change is limited to DOWNLOAD. Backup, restore, upgrade, commit, and
rollback polling retain their current behavior.

### 3. Gate workflow progress on the persisted task result

Automatic workflow readiness for DOWNLOAD must require both:

- the latest download task is persisted as `COMPLETED`; and
- the operational snapshot is `COMPLETE`.

The operational snapshot alone must not advance a workflow from DOWNLOAD to
BACKUP.

### 4. Never cancel a requested download retry from snapshot COMPLETE

The failed-step retry precheck must not change a failed DOWNLOAD task to
`COMPLETED` merely because the current operational snapshot is `COMPLETE`.
A requested download retry must execute the normal retry RPC path. The stronger
version comparison currently used for UPGRADE remains unchanged.

## Error Handling and Observability

- Log rejected terminal messages with a stable reason: task not running,
  missing task start time, missing file name, file mismatch, missing time,
  malformed time, or stale operation time.
- Do not expose SFTP credentials or raw secret-bearing payloads.
- Preserve Kafka ordering and existing task terminal-state protections.
- Do not change database schema or introduce a new Kafka consumer group.

## Tests and Acceptance

Add focused regressions proving:

1. a state-only `COMPLETE` leaves a running download task unchanged;
2. a correlated `update COMPLETE` completes the running task;
3. a correlated `update FAIL` fails the running task;
4. a stale or mismatched terminal notification is ignored;
5. polling `COMPLETE` does not complete a download task, while polling `FAIL`
   still fails it;
6. automatic workflow readiness does not advance from a rollback snapshot when
   the download task is not completed;
7. retrying a failed DOWNLOAD still invokes the download execution path when
   the operational snapshot is `COMPLETE`.

Focused listener, polling, workflow, and batch-upgrade tests must pass before
running the full `device-maintenance` test suite.
