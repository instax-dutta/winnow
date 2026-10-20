#!/usr/bin/env bash
# host-mvn.sh - run a Maven goal on the build host.
#
# The build host is the machine named by SEO_BUILD_HOST. It has a JDK 21 and the Maven
# wrapper, so the build is identical to a local one. This script exists so that a gate can
# be executed from any machine with ssh access to the host.
#
# Usage:
#   export SEO_BUILD_HOST='<the-build-host>'
#   export SEO_BUILD_DIR='<the-directory-on-that-host>'
#   ./tools/verify/host-mvn.sh test
#   ./tools/verify/host-mvn.sh -Dtest=SomeIT test
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
HOST="${SEO_BUILD_HOST:-}"
REMOTE="${SEO_BUILD_DIR:-}"
JAVA_HOME_REMOTE="/usr/lib/jvm/java-21-openjdk-amd64"

if [ -z "$HOST" ]; then
  echo "host-mvn: SEO_BUILD_HOST is not set." >&2
  echo "Set SEO_BUILD_HOST to the machine the build runs on, and SEO_BUILD_DIR to the" >&2
  echo "directory this project owns on it." >&2
  exit 2
fi

if [ -z "$REMOTE" ]; then
  echo "host-mvn: SEO_BUILD_DIR is not set." >&2
  echo "Set SEO_BUILD_DIR to the directory this project owns on $HOST." >&2
  exit 2
fi

# Mirror the working tree to the build host, then run the goal there.
tar czf - --no-xattrs \
    --exclude='.git' \
    --exclude='target' \
    --exclude='node_modules' \
    --exclude='.unlazy' \
    -C "$ROOT" . | ssh "$HOST" "mkdir -p $REMOTE && tar xzf - -C $REMOTE"

ssh "$HOST" "cd $REMOTE && JAVA_HOME=$JAVA_HOME_REMOTE ./mvnw -B $*"
