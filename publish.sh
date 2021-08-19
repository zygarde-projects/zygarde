#!/usr/bin/env bash
./gradlew clean \
  :zygarde:build :zygarde:publish \
  :zygarde-codegen:build :zygarde-codegen:publish \
  :zygarde-codegen-base:build :zygarde-codegen-base:publish \
  :zygarde-codegen-dsl:build :zygarde-codegen-dsl:publish \
  :zygarde-codegen-jpa:build :zygarde-codegen-jpa:publish \
  :zygarde-core:build :zygarde-core:publish \
  :zygarde-extensions-kotlinpoet:build :zygarde-extensions-kotlinpoet:publish \
  :zygarde-jpa:build :zygarde-jpa:publish \
  :zygarde-test:build :zygarde-test:publish \
  -Pversion=1.2.0
