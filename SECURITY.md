# Security policy

## Reporting a vulnerability

**Do not open a public issue for a security problem.**

Use GitHub's private vulnerability reporting: go to the **Security** tab of this
repository, then **Report a vulnerability**. That opens a private channel with the
maintainers.

If private reporting is not available to you, email the maintainer directly. Do not
put the details in a public issue, a pull request, a discussion, or a gist.

Include what you found, how to reproduce it, and what an attacker would gain. A
report that helps us fix it quickly is more valuable than one that scores highly.

## What we will do

- Acknowledge a report within three working days.
- Triage and confirm within ten working days.
- Tell you what we intend to do, and when, if a fix is needed.
- Credit you in the release notes, unless you would rather we did not.
- Tell you when the fix ships.

## What is in scope

Everything in this repository: the application, the container image, the build
workflows, and the release process.

## What is out of scope

- Findings that need an already-compromised host, an already-compromised account,
  or physical access to the machine.
- Denial of service through sheer volume, without a specific amplification.
- Missing hardening headers on an application that is not exposed to the internet.
- The self-hosted scraping feature being used against a site that forbids it. That
  is a question for the site, not for us.

## How this project handles credentials

This project is designed so that a credential never has to live in the database or
in a file that is committed.

- A credential is stored in the secret store. The database row holds a reference.
- The secret is resolved at the moment it is needed and is never cached, logged or
  returned in a response.
- Every file in the repository is scanned for credential-shaped literals on every
  build. A match fails the build.
- A credential that has ever been committed, printed or shipped in an artifact has to be
  rotated at the provider. The application never carries one, and neither does this
  repository.

## Hardening this project

The repository ships with the settings that matter already on:

- Dependabot for the Maven, the container base images and the GitHub Actions.
- CodeQL for static analysis on every push and pull request.
- Secret scanning with push protection.
- Dependency review on every pull request.
- Branch protection on `main`.
- A vulnerability disclosure policy, which is this file.

Read `docs/SECURITY.md` for the threat model and the known limitations.
