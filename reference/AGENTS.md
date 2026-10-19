# reference/

## Purpose

The decompiled original application. It is evidence, not source. It exists so any claim about
what the original did can be checked against real code instead of a summary.

## Ownership

- `decompiled/` - 67 Java classes decompiled from `seo-0.0.1-SNAPSHOT.jar` with CFR 0.152,
  under their original package layout `com/pg/seo/`.
- `decompiled.tar.gz` - the same tree as delivered from the build host.

## Local Contracts

- **Never edit anything in here.** It is a record of a shipped binary.
- Never copy code from here into `src/`. The rebuild is a clean-room implementation. Reading
  the original to understand behaviour is the point; copying it would reproduce the defects.
- The decompilation is lossy. CFR output contains `(Object)` casts, flattened builder chains and
  an odd `equals` implementation. Treat a construct as a hint, not as source of truth, and
  confirm behaviour against `docs/reverse-engineering/`.

## Work Guidance

- To re-decompile after a new artifact: upload the application classes to the build host, jar
  them, and run CFR. The exact commands are in `docs/MAINTENANCE.md`.
- To check a specific class, read it here and cross-reference the analysis document.

## Verification

- No build, no test. The verification is that the tree matches the CFR output that produced it.

## Child DOX Index

This directory has no child DOX files.
