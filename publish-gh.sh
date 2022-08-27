#!/usr/bin/env bash
BUILD_TIME=$(date +"%Y%m%d%H%M%S")
VERSION=${RELEASE_VERSION:-"$BUILD_TIME"}

echo "VERSION=$VERSION"

./gradlew clean \
  publishAllPublicationsToGithubRepository \
  -Pversion="$VERSION"
