#!/usr/bin/env bash
set -e

VERSION=${RELEASE_VERSION:-$(date +"%Y%m%d%H%M%S")}

echo "Publishing to Maven Central with VERSION=$VERSION"

# Check required environment variables
if [ -z "$MAVEN_CENTRAL_TOKEN" ]; then
  echo "Error: MAVEN_CENTRAL_TOKEN is not set"
  echo ""
  echo "Generate a token at: https://central.sonatype.com/account"
  echo "Then base64 encode: echo -n 'username:password' | base64"
  exit 1
fi

if [ -z "$GPG_SIGNING_KEY" ]; then
  echo "Error: GPG_SIGNING_KEY is not set"
  echo ""
  echo "Export your key: gpg --armor --export-secret-keys KEY_ID"
  exit 1
fi

# GPG_SIGNING_PASSWORD is optional (can be empty for keys without passphrase)

# Publish to local staging, then upload to Central Portal
./gradlew clean \
  publishAllPublicationsToLocalStagingRepository \
  publishToMavenCentralPortal \
  -Pversion="$VERSION"

echo ""
echo "Successfully published $VERSION to Maven Central!"
echo "Check status at: https://central.sonatype.com/publishing/deployments"
