# Gates: winnow 0.1.0

OWNS: pom.xml, src/**, docs/**, tools/**, AGENTS.md, GATES.md, PLAN.md, README.md

Scope: The self-hosted cold-email and lead-generation platform, its documentation and its
DOX tree, and the guarantee that no credential exists in source.

The ledger covers every independently required outcome of the request. Each runnable gate has
a `CHECK:` command and an `EXPECT:` token. A gate is met only when the command exits zero and
the expectation matches.

The gates are written before the implementation and are not edited to match the outcome.

- [x] G1: The project compiles from a clean checkout.
  CHECK: ./tools/verify/host-mvn.sh -DskipTests clean compile
  EXPECT: BUILD SUCCESS
  EVIDENCE: automatic-evidence=v1; definition-sha256=0ddb55e5881eddae9e74d5fc3a09eb7cc8e6854dd5a0d3e8e6d3c8c9215ab020; exit=0; EXPECT=matched; output-sha256=00596c909426c042a3c3e21f240cb7d6a3490c43dc9862db4c3c81429ded47fa; output-bytes=1821; shell=/bin/sh; cwd=/Users/saiduttaabhishekdash/winnow; path=b93c3fbfaca3/30 entries

- [x] G2: The whole test suite passes.
  CHECK: ./tools/verify/host-mvn.sh test
  EXPECT: Tests run:
  EVIDENCE: automatic-evidence=v1; definition-sha256=6426bfa424a0fd3905758dffc5c3b37db5338e4058b518234686738813216d20; exit=0; EXPECT=matched; output-sha256=84349eeb4e41fceafb7223ed07635b960a0faf85eca1eb459cb45f589e6288b2; output-bytes=42636; shell=/bin/sh; cwd=/Users/saiduttaabhishekdash/winnow; path=b93c3fbfaca3/30 entries

- [x] G3: The packaged application starts and serves the login page.
  CHECK: node tools/verify/app-boot.mjs
  EXPECT: APP-BOOT-PASS
  EVIDENCE: automatic-evidence=v1; definition-sha256=dd6a845542ed3641d0bb656d6735beb991dd9ae29b97401661896903cfa9fe6c; exit=0; EXPECT=matched; output-sha256=ecdbd2f274af9d3d16fc74b7f0444a2f49982d689fe2861ce62b16437b835c29; output-bytes=318; shell=/bin/sh; cwd=/Users/saiduttaabhishekdash/winnow; path=b93c3fbfaca3/30 entries

- [x] G4: No credential-shaped literal exists in the source tree.
  CHECK: node tools/verify/no-secrets.mjs
  EXPECT: SECRET-SCAN-PASS
  EVIDENCE: automatic-evidence=v1; definition-sha256=d1dc07729775e28c09578d65ff379681c6b7d9f02258d7e4dc05e840baa0546a; exit=0; EXPECT=matched; output-sha256=d9c937f1ad9c468d02b993ee03899d004de09cc749012e609e395f31e0989e7b; output-bytes=59; shell=/bin/sh; cwd=/Users/saiduttaabhishekdash/winnow; path=b93c3fbfaca3/30 entries

- [x] G5: The code obeys the hard rules in the root AGENTS.md.
  CHECK: node tools/verify/code-hygiene.mjs
  EXPECT: HYGIENE-PASS
  EVIDENCE: automatic-evidence=v1; definition-sha256=30319d322e67e8c217154dfcf7040c24dc03a77578b36cbb69075fcf82a7c62a; exit=0; EXPECT=matched; output-sha256=96604b1956e4de90aed1bfc5e360075471f20c7825eb69b4fe29de4054aff10e; output-bytes=43; shell=/bin/sh; cwd=/Users/saiduttaabhishekdash/winnow; path=b93c3fbfaca3/30 entries

- [x] G6: The DOX tree is complete, consistent and reachable from the root.
  CHECK: node tools/verify/dox-tree.mjs
  EXPECT: DOX-TREE-PASS
  EVIDENCE: automatic-evidence=v1; definition-sha256=72ef081e7d36970052f0ac1e445b6fcc7625c75529c9ddadbc467f6b72862c82; exit=0; EXPECT=matched; output-sha256=f2277798baf324e3d3feeddffc97d896219f11d2e0a7a403d8d6cafdba2a5835; output-bytes=65; shell=/bin/sh; cwd=/Users/saiduttaabhishekdash/winnow; path=b93c3fbfaca3/30 entries

- [x] G7: One authenticated tenant cannot read or write another tenant's data.
  CHECK: ./tools/verify/host-mvn.sh -Dtest=TenantIsolationIT test
  EXPECT: Tests run:
  EVIDENCE: automatic-evidence=v1; definition-sha256=acd69fda76c796dd1b8cb9306b17d05a01f14ae73321f180386080520ca1da03; exit=0; EXPECT=matched; output-sha256=955716e66b689683656b44febb5a81758b0ff3ed213f36c75c1870a79d88184a; output-bytes=32480; shell=/bin/sh; cwd=/Users/saiduttaabhishekdash/winnow; path=b93c3fbfaca3/30 entries

- [x] G8: Every Flyway migration applies to a clean MySQL 8 schema.
  CHECK: node tools/verify/migrations.mjs
  EXPECT: MIGRATIONS-PASS
  EVIDENCE: automatic-evidence=v1; definition-sha256=f9d5ca79bca148c9d4b50eb7971b5abe6c16099a6c0372d8406c34e57cf5bcbe; exit=0; EXPECT=matched; output-sha256=b6f928018985213686763432049185eb60bc3f83c0ed4031e9d2a907a239e435; output-bytes=532; shell=/bin/sh; cwd=/Users/saiduttaabhishekdash/winnow; path=b93c3fbfaca3/30 entries

- [x] G9: Every HTTP endpoint that mutates state rejects a request without a CSRF token.
  CHECK: ./tools/verify/host-mvn.sh -Dtest=CsrfProtectionIT test
  EXPECT: Tests run:
  EVIDENCE: automatic-evidence=v1; definition-sha256=049dad7cef65acc9c8122910b5596fe92cf7fba79830958b6985bca648874a48; exit=0; EXPECT=matched; output-sha256=346883cf6dcbd32c1670e2f7ed3c753fecdfe5d1183edf2ef5b1221e1582d641; output-bytes=32902; shell=/bin/sh; cwd=/Users/saiduttaabhishekdash/winnow; path=b93c3fbfaca3/30 entries

- [x] G10: Every link in the public documents resolves to a file that exists.
  CHECK: node tools/verify/readme-links.mjs
  EXPECT: LINK-PASS
  EVIDENCE: automatic-evidence=v1; definition-sha256=ddf491a0209a174885efd5b328e3294a9b157c20bcb433c566ce876a1987a380; exit=0; EXPECT=matched; output-sha256=b55a2bc337c67afd71de1adc225699b80ee453d3afe80cd5251254ade940e3cc; output-bytes=59; shell=/bin/sh; cwd=/Users/saiduttaabhishekdash/winnow; path=b93c3fbfaca3/30 entries

- [x] G11: The architecture, security and maintenance documents exist and are non-empty.
  CHECK: node tools/verify/docs.mjs
  EXPECT: DOCS-PASS
  EVIDENCE: automatic-evidence=v1; definition-sha256=3bdcd247582af9720632a59de381b691cd8c7354362b4de9b7e309f28e98d496; exit=0; EXPECT=matched; output-sha256=3f5a1a8539ad2fb443c4ab076e9f2434aa0e92f952eb5995c0abbed6ba7e8d8c; output-bytes=57; shell=/bin/sh; cwd=/Users/saiduttaabhishekdash/winnow; path=b93c3fbfaca3/30 entries

- [x] G12: No tracked file refers to an earlier version of the software or to removed material.
  CHECK: node tools/verify/parity.mjs
  EXPECT: PARITY-PASS
  EVIDENCE: automatic-evidence=v1; definition-sha256=cf1c5c847ab21b41eec4a769a97502c596a8988be0e1fa1d576bc912d02b9691; exit=0; EXPECT=matched; output-sha256=58a6b85b51cf40963b77b5e54a39d1edecb610a0209dcfbf4b4d7cd31bb596da; output-bytes=76; shell=/bin/sh; cwd=/Users/saiduttaabhishekdash/winnow; path=b93c3fbfaca3/30 entries

- [x] G13: A fresh clone of the repository builds without the local Maven cache.
  CHECK: node tools/verify/clean-build.mjs
  EXPECT: CLEAN-BUILD-PASS
  EVIDENCE: automatic-evidence=v1; definition-sha256=7ab7397f9c36492f6fcc2445447b2e3d6dab1b675ecd2fc828c841f044cf8176; exit=0; EXPECT=matched; output-sha256=8ac33224aae4c2d2ad71d34d92ab1b9667579aee7316b67edcfafc930f65aeb1; output-bytes=323; shell=/bin/sh; cwd=/Users/saiduttaabhishekdash/winnow; path=b93c3fbfaca3/30 entries

<!--
Notes for the checker:
- G1, G2, G7 and G9 run Maven through tools/verify/host-mvn.sh, which mirrors the working
  tree to the build host named by SEO_BUILD_HOST and runs the Maven wrapper there with
  JDK 21. The repository ships the wrapper, so no system Maven is required anywhere.
- G3, G8 and G13 need the MySQL 8 container and the development credentials in
  .env.development, which is git-ignored. Each of them says so explicitly and fails rather
  than passing silently when the credentials are absent.
- G10 and G12 scan the tracked tree and fail on the first thing they find.
-->
