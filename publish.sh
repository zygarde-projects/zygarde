#!/usr/bin/env bash
./gradlew build bintrayUpload -Pversion="$1"
