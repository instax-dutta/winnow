#!/usr/bin/env bash
# sync-and-build.sh - mirror the working tree to the build host and compile there.
#
# The build host is tejes@pelican. Everything for this project lives under
# ~/seo-rebuild on that host. Nothing else running on the host is touched.
#
# Usage:
#   ./tools/sync-and-build.sh            # sync + mvn compile
#   ./tools/sync-and-build.sh test       # sync + mvn test
#   ./tools/sync-and-build.sh verify     # sync + mvn verify
#   ./tools/sync-and-build.sh package    # sync + mvn package
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
HOST="${SEO_BUILD_HOST:-tejes@pelican}"
REMOTE="${SEO_BUILD_DIR:-~/seo-rebuild/app}"
GOAL="${1:-compile}"

echo "==> syncing $ROOT -> $HOST:$REMOTE"
tar czf - \
    --exclude='.git' \
    --exclude='target' \
    --exclude='node_modules' \
    --exclude='.unlazy' \
    --exclude='reference' \
    -C "$ROOT" . | ssh "$HOST" "mkdir -p $REMOTE && tar xzf - -C $REMOTE"

echo "==> mvn $GOAL on $HOST"
ssh "$HOST" "cd $REMOTE && JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 ./mvnw -B $GOAL"
