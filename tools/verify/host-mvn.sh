#!/usr/bin/env bash
# host-mvn.sh - run a Maven goal on the build host.
#
# The build host is tejes@pelican. It has a JDK 21 and the Maven wrapper, so the build
# is identical to a local one. This script exists so that a gate can be executed from
# any machine with ssh access to the host.
#
# Usage: ./tools/verify/host-mvn.sh test
#        ./tools/verify/host-mvn.sh -Dtest=SomeIT test
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
HOST="${SEO_BUILD_HOST:-tejes@pelican}"
REMOTE="${SEO_BUILD_DIR:-~/seo-rebuild/app}"
JAVA_HOME_REMOTE="/usr/lib/jvm/java-21-openjdk-amd64"

# Mirror the working tree to the build host, then run the goal there.
tar czf - \
    --exclude='.git' \
    --exclude='target' \
    --exclude='node_modules' \
    --exclude='.unlazy' \
    --exclude='reference' \
    -C "$ROOT" . | ssh "$HOST" "mkdir -p $REMOTE && tar xzf - -C $REMOTE"

ssh "$HOST" "cd $REMOTE && JAVA_HOME=$JAVA_HOME_REMOTE ./mvnw -B $*"
