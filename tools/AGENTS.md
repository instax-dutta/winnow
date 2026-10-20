# tools/

## Purpose

Repository tooling. Scripts that are run by a human or by CI against this repository, not part
of the shipped application.

## Ownership

- `sync-and-build.sh` - mirrors the working tree to the build host named by
  `SEO_BUILD_HOST` and runs a Maven goal there.
- `verify/app-boot.mjs` - proves the packaged application starts and serves its login page.
- `verify/clean-build.mjs` - proves a fresh copy of the repository builds with no local
  Maven cache.
- `verify/code-hygiene.mjs` - enforces the hard rules from the root `AGENTS.md` against the
  source tree.
- `verify/dev-env.mjs` - loads the git-ignored development credentials.
- `verify/docs.mjs` - checks that the documentation set exists and is complete.
- `verify/dox-tree.mjs` - checks that the DOX tree is complete, consistent and reachable.
- `verify/host-mvn.sh` - runs a Maven goal on the build host.
- `verify/migrations.mjs` - proves the Flyway migrations apply to a clean MySQL 8.
- `verify/no-secrets.mjs` - fails if a tracked file holds a credential-shaped literal.
- `verify/parity.mjs` - fails if a tracked file refers to an earlier version of the software
  or to material that has been removed.

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

- Run the tool and check the output. `node tools/verify/no-secrets.mjs` must report the
  same file count on an unchanged tree.

## Child DOX Index

This directory has no child DOX files.
