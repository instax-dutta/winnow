# docs/reverse-engineering/

## Purpose

The complete analysis of the original `seo-0.0.1-SNAPSHOT` artifact. This is the ground truth
for what the original application did. Read it before changing any behaviour that the original
defined, so a change is a deliberate decision and not an accidental regression.

## Ownership

| File | Covers |
|---|---|
| `01-services-and-scraper.md` | `EmailService`, `GmailService`, the two extractors, `EmailInboxService`, the Amazon scraper, `PuppeteerRunner`, `SchedulerConfig`, the JPA and MVC configuration |
| `02-controllers-and-security.md` | The full endpoint table, the authentication flow, the authorization model, the licence check, file upload/download, and every security weakness found |
| `03-entities-and-templates.md` | Every entity and repository, the DTOs, the proposed modern schema, the schema defects, and the template/IA analysis |

## Local Contracts

- These documents describe the original, not the rebuild. Never edit them to describe new
  behaviour. New behaviour is documented in `docs/ARCHITECTURE.md`.
- Every finding quotes the original code or the original response so it can be checked
  against `reference/decompiled/`.
- Where the original was broken, the finding says so explicitly. "The original does X" and
  "X is correct" are different statements, and the rebuild may differ from the original.

## Work Guidance

- When the rebuild deliberately differs from the original, note the difference in
  `docs/MIGRATION-FROM-0.0.1.md` and point at the finding here.
- If a new question about the original arises, add it to the relevant file rather than
  creating a fourth document.

## Verification

- Cross-check any cited line against `reference/decompiled/com/pg/seo/`. The decompiled source
  is the evidence; these documents are the reading of it.

## Child DOX Index

This directory has no child DOX files.
