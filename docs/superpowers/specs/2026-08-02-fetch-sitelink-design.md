# fetchSiteLink Design

## Goal

Provide a reusable, read-only PowerShell command for inspecting one SiteLink on DCI environment 112 or 114. The command retrieves the SiteLink and its referenced physical resources, saves raw evidence, runs deterministic consistency checks, and generates a Mermaid topology report without using AI.

## User Interface

After one-time command installation, the primary interface is:

```powershell
fetchSiteLink 112 "<SiteLinkID>"
fetchSiteLink 114 "<SiteLinkID>"
```

The script remains directly executable without installation:

```powershell
.\scripts\fetchSiteLink.ps1 112 "<SiteLinkID>"
```

The environment argument accepts only `112` or `114`. The SiteLink ID must begin with `SiteLink-`. SSH requests the password interactively in the terminal, so the password is not passed as a command-line argument, written to output, or stored by the script.

## Files

- `scripts/fetchSiteLink.ps1`: validates input, connects to the selected environment, runs the remote read-only query, transforms the result, performs checks, and writes artifacts.
- `scripts/install-fetchSiteLink-command.ps1`: installs a small PowerShell profile function named `fetchSiteLink` that delegates to the main script by absolute path. It must preserve existing profile content and be safe to run repeatedly.
- `scripts/tests/fetchSiteLink.Tests.ps1`: Pester tests for argument validation, deterministic topology transformation, checks, Mermaid rendering, and installer idempotence. Live SSH tests are opt-in and are not part of the default test run.

The scripts live in the DCI workspace root because they operate across deployed controller/platform data rather than belonging to one production repository. This design record is kept in `controller/bytedance` for review and traceability.

## Environment Configuration

The script owns a fixed environment map rather than accepting arbitrary hosts:

| Environment | SSH endpoint | SSH user |
| --- | --- | --- |
| `112` | `116.128.200.229:13112` | `actsvt112` |
| `114` | `116.128.200.229:13114` | `actsvt112` |

The script does not contain MongoDB credentials. On the remote host it reads the active service's `mongodb.properties`, exports the values only for the lifetime of the remote shell, invokes `mongosh --quiet`, and unsets the credential variables before exit.

## Read-Only Data Collection

The remote query reads, but never updates, these collections:

- `config-site-link`: exact SiteLink, source/destination, supporting links, A/Z external add-drop links, properties, implementation state, explicit routes, ERO objects, and route XCs.
- `config-phy-node`: every physical node referenced by the SiteLink ID, ERO, route XCs, supporting links, and external links; equipment, termination points, cross-connections, and internal links are included.
- `config-site-node`: referenced sites, friendly names, and site types for labels and diagnostics.

The remote process emits one versioned JSON document to stdout. Diagnostic text goes to stderr so it cannot corrupt the JSON payload. The local script validates the schema version before transforming it.

## Local Data Model

The transformation creates a deterministic inspection model with:

- SiteLink metadata: ID, friendly name, bandwidth, grid, protection type, model, A/Z type, creation time, and implementation state.
- Ordered route legs: primary, secondary, and third, each preserving ERO index order.
- Nodes: route-local role, site, NE, node type, equipment type, configured type, class mode, shelf, slot, and friendly name.
- Hops: TP hops and physical/logical link hops with their original IDs.
- XCs: route ID, physical match status, source/destination TP sets, APS/amplifier/fixed flags, and endpoint ownership.
- Internal links: route, supporting, external, and physical-node IDs, with exact-match status.

Raw IDs are retained even when friendly labels are available. The report never replaces evidence with inferred labels.

## Deterministic Checks

The script reports `PASS`, `WARN`, or `FAIL` for:

1. Every route XC has an exact physical XC ID match.
2. Every physical link requested by the route/supporting/external resource sets exists as a physical-node internal link or topology link as appropriate.
3. A virtual endpoint such as `MPO` is distinguished from numbered physical endpoints such as `MPO1..MPO4` and `MPO1..MPO8`.
4. FMUX32 MPO fan-in contains exactly `MPO1..MPO4`; CMUX64 aggregation contains exactly `MPO1..MPO8` when that card model applies.
5. Route, supporting-link, and A/Z external resource sets are compared, with missing or extra MPO members listed explicitly.
6. Main, secondary, and third route legs preserve ERO order and do not silently share an incompatible endpoint resource.
7. Referenced nodes, equipment, TPs, XCs, and links that are absent from queried physical data are listed by full ID.
8. The persisted SiteLink implementation state is highlighted when resource checks fail, exposing partial or false-success states.

Checks are observational. The command never repairs, implements, deimplements, or updates data.

## Mermaid Rendering

Known endpoint and transit patterns are rendered using fixed rules:

- MUXPANEL and FMUX32/CMUX64 equipment form endpoint fan-in/fan-out groups.
- TILA with endpoint class mode is rendered inside the A/Z OTM subgraph.
- TILA with ILA class mode is rendered as an ILA transit site.
- DGE equipment is rendered as a DGE transit site.
- Primary paths use thick arrows and protection paths use dashed arrows.
- XC and internal-link labels are derived from actual TP suffixes, such as `MPO1-4`, `SIG -> COM1`, or `SIGA -> LINE_WEST`.

When a topology does not match a known rule, the renderer falls back to a generic ERO graph. Every TP/link hop is shown in index order, and unknown equipment is labeled with its actual type and slot. The fallback does not guess OTM, ILA, DGE, main, or protection semantics.

Mermaid node IDs are generated independently from DCI resource IDs and escaped before output. Full resource IDs appear in evidence tables rather than Mermaid identifiers, avoiding invalid syntax and unreadable diagrams.

## Output

Each run creates a unique directory:

```text
artifacts/sitelink/<environment>/<yyyyMMdd-HHmmss>-<sanitized-friendly-name>/
|-- raw.json
`-- report.md
```

`raw.json` contains the versioned remote query result. `report.md` contains:

1. invocation environment and retrieval time;
2. SiteLink metadata;
3. PASS/WARN/FAIL summary;
4. Mermaid topology;
5. route XC versus physical XC comparisons;
6. route/supporting/external link comparisons;
7. missing-resource details with full IDs.

The terminal prints a concise result summary and the absolute paths of both files. Generated artifacts are local diagnostics and must be excluded from repository commits.

## Failure Handling

- Unsupported environment or malformed SiteLink ID: fail before SSH with exit code `2`.
- SSH/authentication failure: emit a concise remediation message and exit nonzero without creating a report.
- Missing remote Mongo configuration or `mongosh`: exit nonzero and identify the missing prerequisite without printing credentials.
- SiteLink not found: write no topology report and exit with code `4`.
- Partial referenced data: preserve `raw.json`, generate `report.md`, mark affected checks `FAIL`, and exit with code `5`.
- Mermaid rendering failure: preserve `raw.json`, write the evidence/check sections, explain the rendering error, and exit with code `6`.

Temporary local files are written only inside the final run directory. A failed run removes incomplete temporary payloads while preserving a valid `raw.json` when collection succeeded.

## Testing

Default tests use fixture JSON and do not access 112 or 114. Fixtures cover:

- 32-wave unprotected single-FMUX32;
- 64-wave unprotected dual-FMUX32;
- 32-wave protected single-FMUX32 with two TILA paths;
- 64-wave protected dual-FMUX32;
- commercial-C CMUX64 with eight-port aggregation;
- missing `MPO5..MPO8` internal links;
- route XC ID mismatch;
- unknown topology fallback;
- malformed JSON and missing SiteLink.

Tests assert stable check codes and key Mermaid edges rather than comparing an entire Markdown file. An opt-in live smoke test accepts `112` or `114`, prompts for SSH authentication, fetches one user-supplied SiteLink, and verifies only that valid artifacts and schema are produced.

## Non-Goals

- No AI or network service is used for topology interpretation.
- No remote or local database mutation.
- No automatic implement/deimplement or repair operation.
- No storage of SSH or MongoDB credentials.
- No image/PDF rendering; the deliverable is Mermaid Markdown plus raw JSON.
- No attempt to infer unsupported topology semantics beyond the ordered ERO fallback.
