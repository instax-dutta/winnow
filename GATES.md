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
  CHECK: ./mvnw -q -B -DskipTests clean compile
  EXPECT: BUILD SUCCESS
  EVIDENCE: pending

- [ ] G2: The whole test suite passes.
  CHECK: ./mvnw -B test
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
  CHECK: ./mvnw -B test -Dtest='TenantIsolationIT'
  EXPECT: Tests run:
  EVIDENCE: pending

- [ ] G8: Every Flyway migration applies to a clean MySQL 8 schema.
  CHECK: node tools/verify/migrations.mjs
  EXPECT: MIGRATIONS-PASS
  EVIDENCE: pending

- [ ] G9: Every HTTP endpoint that mutates state rejects a request without a CSRF token.
  CHECK: ./mvnw -B test -Dtest='CsrfProtectionIT'
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
- G1, G2, G7 and G9 run Maven, which needs a JDK 21 on PATH and network access for the
  first dependency resolution. The repository ships the Maven wrapper, so no system Maven
  is required.
- G3 and G8 need the dedicated MySQL 8 container on the build host. Both are skipped with
  an explicit SKIP notice rather than a silent pass when the database is unreachable.
- G10 writes SECRETS.txt, which is git-ignored. The gate asserts the count, not the content.
-->
