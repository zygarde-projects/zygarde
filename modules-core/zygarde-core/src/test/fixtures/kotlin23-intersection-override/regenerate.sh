#!/usr/bin/env bash
# Rebuilds the Kotlin 2.3 compiled fixture jar used by zygarde-core and zygarde-model-mapping-codegen-dsl tests.
set -euo pipefail
cd "$(dirname "$0")"
ROOT="$(cd ../../../../../.. && pwd)"
"$ROOT/gradlew" -p . clean jar --no-daemon -q
JAR=build/libs/kotlin23-intersection-override.jar
for target in \
  "$ROOT/modules-core/zygarde-core/src/test/resources/fixtures" \
  "$ROOT/modules-model-mapping/zygarde-model-mapping-codegen-dsl/src/test/resources/fixtures"; do
  mkdir -p "$target"
  cp "$JAR" "$target/kotlin23-intersection-override.jar"
done
rm -rf build .gradle .kotlin
