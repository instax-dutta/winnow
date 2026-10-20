# docs/

## Purpose

The durable documentation of the project: what it is, how it is built, how it was migrated
from the original, what its security model is, and what the reverse engineering found.

## Ownership

| File | Covers |
|---|---|
| `ARCHITECTURE.md` | The system: layers, data model, request and send flows, configuration surface |
| `MIGRATION-FROM-0.0.1.md` | Table-by-table and endpoint-by-endpoint mapping from the original |
| `SECURITY.md` | The threat model, the credential handling rules, the fixes applied to the original's holes |
| `MAINTENANCE.md` | How to build, deploy, migrate and troubleshoot, including the remote build host |
| `SECRET-HARVEST.md` | What the original leaked, how it was found, and the rotation order |
| `EXPLAINER.html` | A plain-language walkthrough for a non-engineer: how the original worked, what was wrong, what changed |
| `reverse-engineering/` | The full decompilation analysis of the original |

## Local Contracts

- A document records a decision or a procedure, not a status update.
- `EXPLAINER.html` is the one document written for a reader who does not read code. It is
  still factual: every old behaviour it describes cites the same evidence the other
  documents cite. It is a single self-contained file with no external dependency, so it
  opens from disk with no server and no network.
- Every claim about the original cites the evidence: a decompiled file and line, a table, or
  a captured response.
- When the code changes, the matching document changes in the same commit. A document that
  describes removed behaviour is deleted, not annotated.
- Measured numbers carry their provenance: the environment, the command and the date.

## Work Guidance

- New subsystem: add a section to `ARCHITECTURE.md` rather than a new file.
- New security-relevant decision: add it to `SECURITY.md`.
- Fixing a defect that the original also had: record it in `MIGRATION-FROM-0.0.1.md` so the
  reason survives.

## Verification

- No automated check. A document review is the check. Links must resolve.

## Child DOX Index

| Path | Scope |
|---|---|
| `docs/reverse-engineering/` | The decompilation record of seo-0.0.1-SNAPSHOT |
