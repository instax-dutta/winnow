# tools/

## Purpose

Repository tooling. Scripts that are run by a human or by CI against this repository, not part
of the shipped application.

## Ownership

- `harvest-secrets.mjs` - scans the original artifact set for hardcoded credentials and writes
  the inventory to `SECRETS.txt`.

## Local Contracts

- A tool reads from an explicit path and writes to an explicit path. No hidden defaults, no
  ambient state, no network access unless the tool says so.
- A tool that finds secrets writes them to a git-ignored file, never to a tracked one.
- A tool is idempotent: running it twice produces the same result.
- Tools are plain Node with no dependencies, so they run anywhere Node runs.

## Work Guidance

- Add a tool only when a task is repeated or needs to be reproducible. A one-off command
  belongs in the shell history, not here.
- A tool that writes output names the output path in its usage block.

## Verification

- Run the tool and check the output. `node tools/harvest-secrets.mjs` must report the same
  finding count on an unchanged artifact.

## Child DOX Index

This directory has no child DOX files.
