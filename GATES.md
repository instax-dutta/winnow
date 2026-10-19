# Gates: seo-0.1.0 rebuild

OWNS: pom.xml, src/**, docs/**, tools/**, AGENTS.md, GATES.md, PLAN.md, README.md

Scope: A clean-room rebuild of the seo-0.0.1-SNAPSHOT cold-email platform that keeps the
product surface, fixes every defect found in the reverse engineering, ships the documentation
and DOX tree a future maintainer needs, and carries no credential in source.

The ledger covers every independently required outcome of the request. Each runnable gate has
a `CHECK:` command and an `EXPECT:` token. A gate is met only when the command exits zero and
the expectation matches.

The gates are written before the implementation and are not edited to match the outcome.

- [ ] G1: The project compiles from a clean checkout.
  CHECK: ./tools/verify/host-mvn.sh -q -DskipTests clean compile
  EXPECT: BUILD SUCCESS
  EVIDENCE: pending

- [ ] G2: The whole test suite passes.
  CHECK: ./tools/verify/host-mvn.sh test
  EXPECT: Tests run:
  EVIDENCE: pending

- [ ] G3: The packaged application starts and serves the login page.
  CHECK: node tools/verify/app-boot.mjs
  EXPECT: APP-BOOT-PASS
  EVIDENCE: pending

- [ ] G4: No credential-shaped literal exists in the source tree.
  CHECK: node tools/verify/no-secrets.mjs
  EXPECT: SECRET-SCAN-PASS
  EVIDENCE: pending

- [ ] G5: The code obeys the hard rules in the root AGENTS.md.
  CHECK: node tools/verify/code-hygiene.mjs
  EXPECT: HYGIENE-PASS
  EVIDENCE: pending

- [ ] G6: The DOX tree is complete, consistent and reachable from the root.
  CHECK: node tools/verify/dox-tree.mjs
  EXPECT: DOX-TREE-PASS
  EVIDENCE: pending

- [ ] G7: One authenticated tenant cannot read or write another tenant's data.
  CHECK: ./tools/verify/host-mvn.sh -Dtest=TenantIsolationIT test
  EXPECT: Tests run:
  EVIDENCE: pending

- [ ] G8: Every Flyway migration applies to a clean MySQL 8 schema.
  CHECK: node tools/verify/migrations.mjs
  EXPECT: MIGRATIONS-PASS
  EVIDENCE: pending

- [ ] G9: Every HTTP endpoint that mutates state rejects a request without a CSRF token.
  CHECK: ./tools/verify/host-mvn.sh -Dtest=CsrfProtectionIT test
  EXPECT: Tests run:
  EVIDENCE: pending

- [ ] G10: The original artifact's secrets are still inventoried for the owner.
  CHECK: node tools/harvest-secrets.mjs
  EXPECT: findings:
  EVIDENCE: pending

- [ ] G11: The architecture, migration, security and maintenance documents exist and are non-empty.
  CHECK: node tools/verify/docs.mjs
  EXPECT: DOCS-PASS
  EVIDENCE: pending

- [ ] G12: The original's behaviour parity surface is documented, not guessed.
  CHECK: node tools/verify/parity.mjs
  EXPECT: PARITY-PASS
  EVIDENCE: pending

- [ ] G13: A fresh clone of the repository builds without the local Maven cache.
  CHECK: node tools/verify/clean-build.mjs
  EXPECT: CLEAN-BUILD-PASS
  EVIDENCE: pending

<!--
Notes for the checker:
- G1, G2, G7 and G9 run Maven on the build host through tools/verify/host-mvn.sh, which
  mirrors the working tree to tejes@pelican and runs the Maven wrapper there with JDK 21.
  The repository ships the wrapper, so no system Maven is required anywhere.
- G3, G8 and G13 need the dedicated MySQL 8 container on the build host and the development
  credentials in .env.development, which is git-ignored. Each of them says so explicitly
  and fails rather than passing silently when the credentials are absent.
- G10 writes SECRETS.txt, which is git-ignored. The gate asserts the count, not the content.
-->
