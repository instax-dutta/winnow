#!/usr/bin/env bash
# sync-and-build.sh - mirror the working tree to the build host and compile there.
#
# The build host is the machine named by SEO_BUILD_HOST. Everything for this project lives
# under the directory named by SEO_BUILD_DIR on that host. Nothing else running on the
# host is touched.
#
# Usage:
#   export SEO_BUILD_HOST='<the-build-host>'
#   export SEO_BUILD_DIR='<the-directory-on-that-host>'
#   ./tools/sync-and-build.sh            # sync + mvn compile
#   ./tools/sync-and-build.sh test       # sync + mvn test
#   ./tools/sync-and-build.sh verify     # sync + mvn verify
#   ./tools/sync-and-build.sh package    # sync + mvn package
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
HOST="${SEO_BUILD_HOST:-}"
REMOTE="${SEO_BUILD_DIR:-}"
GOAL="${1:-compile}"

if [ -z "$HOST" ]; then
  echo "sync-and-build: SEO_BUILD_HOST is not set." >&2
  echo "Set SEO_BUILD_HOST to the machine the build runs on, and SEO_BUILD_DIR to the" >&2
  echo "directory this project owns on it." >&2
  exit 2
fi

if [ -z "$REMOTE" ]; then
  echo "sync-and-build: SEO_BUILD_DIR is not set." >&2
  echo "Set SEO_BUILD_DIR to the directory this project owns on $HOST." >&2
  exit 2
fi

echo "==> syncing $ROOT -> $HOST:$REMOTE"
tar czf - --no-xattrs \
    --exclude='.git' \
    --exclude='target' \
    --exclude='node_modules' \
    --exclude='.unlazy' \
    -C "$ROOT" . | ssh "$HOST" "mkdir -p $REMOTE && tar xzf - -C $REMOTE"

echo "==> mvn $GOAL on $HOST"
ssh "$HOST" "cd $REMOTE && JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 ./mvnw -B $GOAL"
