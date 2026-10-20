# docs/

## Purpose

The durable documentation of the project: what it is, how it is built, how it is operated,
and what its security model is.

## Ownership

| File | Covers |
|---|---|
| `EXPLAINER.html` | A plain-language walkthrough of the whole system for a reader who does not read code |
| `ARCHITECTURE.md` | The system: layers, data model, request and send flows, configuration surface |
| `SECURITY.md` | The threat model and the credential handling rules |
| `MAINTENANCE.md` | How to build, deploy, migrate and troubleshoot |

## Local Contracts

- A document records a decision or a procedure, not a status update.
- `EXPLAINER.html` is the one document written for a reader who does not read code. It is
  still factual, and it never contradicts the other documents. It is a single self-contained
  file with no external dependency, so it opens from disk with no server and no network.
- Every claim about the system cites the code it describes, by package and behaviour.
- When the code changes, the matching document changes in the same commit. A document that
  describes removed behaviour is deleted, not annotated.
- No document refers to a previous version of the software. The application is documented as
  it is, and the reason a rule exists is stated as the consequence of breaking it.
- Measured numbers carry their provenance: the environment, the command and the date.

## Work Guidance

- New subsystem: add a section to `ARCHITECTURE.md` rather than a new file.
- New security-relevant decision: add it to `SECURITY.md`.
- Fixing a defect: record it in the document that covers the affected area, so the reason
  survives.

## Verification

- No automated check. A document review is the check. Links must resolve.

## Child DOX Index

| Path | Scope |
|---|---|
