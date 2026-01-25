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

echo "Step 1: Building and staging artifacts..."
./gradlew clean \
  publishAllPublicationsToLocalStagingRepository \
  -Pversion="$VERSION"

echo ""
echo "Step 2: Creating bundle zip..."
./gradlew zipStagingRepository

echo ""
echo "Step 3: Uploading to Maven Central..."
RESPONSE=$(curl -s -w "\n%{http_code}" -X POST \
  "https://central.sonatype.com/api/v1/publisher/upload?publishingType=AUTOMATIC" \
  -H "Authorization: Bearer $MAVEN_CENTRAL_TOKEN" \
  -F "bundle=@build/distributions/bundle.zip")

HTTP_CODE=$(echo "$RESPONSE" | tail -1)
BODY=$(echo "$RESPONSE" | sed '$d')

if [ "$HTTP_CODE" -eq 201 ]; then
  echo "Upload successful!"
  echo "Deployment ID: $BODY"
  echo ""
  echo "Check status at: https://central.sonatype.com/publishing/deployments"
  echo "Artifacts will be available on Maven Central after validation and publishing."
else
  echo "Upload failed with HTTP $HTTP_CODE"
  echo "Response: $BODY"
  exit 1
fi
