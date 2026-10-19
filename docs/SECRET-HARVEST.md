# Secret Harvest - seo-0.0.1-SNAPSHOT

**Deliverable:** `SECRETS.txt` (repo root, git-ignored - contains live credentials)
**Generator:** `tools/harvest-secrets.mjs` (re-runnable, deterministic)

## Result

| Severity | Count | Meaning |
|---|---|---|
| CRITICAL | 51 | Rotate/revoke now. Grants access or cost to a third party. |
| HIGH | 5 | Rotate before reuse. Tied to a person or a machine. |
| MEDIUM | 13 | Personal/business data - data-protection obligation. |
| LOW | 4 | Structural information. |
| **Total** | **73** | |

## What was scanned

1. `BOOT-INF/classes/application.properties` - config secrets
2. `emailsend21.sql` - seeded database credentials (16 tables)
3. `reference/decompiled/**/*.java` - 67 decompiled classes, literal scan
4. `templates/*.html` + `puppeteer/*.js` - shipped front-end and scraper assets

## Headline findings

- **MySQL root password in plaintext** in `application.properties`, alongside username `root`.
- **2 Google OAuth clients** fully compromised: client id, client secret, refresh token and
  live access token for `elexyt24@gmail.com` and `saas@seoagencey.com`.
- **41 Gmail/Hostinger SMTP app passwords** in `email_master` - one per sending account.
- **Licence JWT** shipped in config. The code only *decodes* it and checks `exp`; the
  HS256 signature is never verified, so the licence is trivially forgeable.
- **MAC-address lock** (`mac=D8-BB-C1-5E-BC-88`) - the app refuses to authenticate on any
  other machine. Machine-bound licensing, not a security control.
- **WordPress REST API token** in config.
- **3 bcrypt password hashes** (two are the *same* hash reused across two accounts)
  plus PAN and DOB for the operators.
- **3,583 scraped contact emails** in `email_extractor_data`.

## Negative results (verified clean)

- No private keys, `.pem`, `.p12`, `.jks`, `.keystore` files anywhere in the JAR.
- No AWS / Stripe / Slack / SendGrid / Mailgun credentials.
- No `AIza...` Google API keys, no `sk_live_` keys.
- Templates and the Puppeteer scraper contain no embedded secrets.
- No credentials hardcoded in Java - the code reads them from DB and config only.
- One `AKIA...` hit was a false positive: a byte pattern inside the vendored
  `quickjs-emscripten` WASM blob, not a real AWS key.

## Rotation order

1. Revoke the 2 Google OAuth clients and delete `gmail_master` refresh tokens.
2. Revoke the 41 SMTP app passwords (Google Workspace admin -> mailboxes -> app passwords).
3. Rotate the MySQL root password; drop `root` usage from the application.
4. Rotate the WordPress API token.
5. Delete the licence JWT and the MAC lock; replace with a real signed check or remove.
6. Force password reset for the 3 `login` accounts (hashes are salted bcrypt, cost 12,
   but the reuse of one hash across two accounts must be broken).
