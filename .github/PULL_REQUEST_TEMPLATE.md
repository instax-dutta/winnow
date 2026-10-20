## What this changes

<!-- One paragraph. What was wrong, and what is different now. -->

## Why

<!-- The problem this solves, or the defect it fixes. If this fixes a defect the original
     application also had, say so here and add a line to docs/MIGRATION-FROM-0.0.1.md. -->

## How it was verified

<!-- Which gates you ran, and what the result was. The gate ledger is the contract:

     node ~/.agents/skills/unlazy/scripts/gate-check.mjs --status GATES.md

     If you changed a package, mention the folder AGENTS.md you read. -->

## Checklist

- [ ] Every gate in `GATES.md` passes.
- [ ] The layering holds: nothing points the way the contract says it must not.
- [ ] Every service method takes the tenant id, and every query filters on it.
- [ ] No credential is in the database, in a configuration file, or in a committed file.
- [ ] No `@GetMapping` changes state.
- [ ] Every outbound call has a timeout, and every thread pool is bounded.
- [ ] New or changed behaviour is written down in the document that covers it.
- [ ] A test came with the change.

## Notes for the reviewer

<!-- Anything you want a second opinion on. -->
