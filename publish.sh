#!/usr/bin/env bash
./gradlew clean \
  :zygarde:build :zygarde:publish \
  :zygarde-codegen:build :zygarde-codegen:publish \
  :zygarde-codegen-dsl:build :zygarde-codegen-dsl:publish \
  :zygarde-extensions-kotlinpoet:build :zygarde-extensions-kotlinpoet:publish \
  :zygarde-test:build :zygarde-test:publish \
  -Pversion=1.1.24
