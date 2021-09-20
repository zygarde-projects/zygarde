#!/bin/bash

./gradlew :zygarde-core:dependencies > deps/zygarde-core.txt
./gradlew :zygarde-codegen-base:dependencies > deps/zygarde-codegen-base.txt
./gradlew :zygarde-codegen-dsl:dependencies > deps/zygarde-codegen-dsl.txt
./gradlew :zygarde-jpa:dependencies > deps/zygarde-jpa.txt
./gradlew :zygarde-jpa-codegen:dependencies > deps/zygarde-jpa-codegen.txt
./gradlew :zygarde-model-mapping:dependencies > deps/zygarde-model-mapping.txt
./gradlew :zygarde-model-mapping-codegen:dependencies > deps/zygarde-model-mapping-codegen.txt
./gradlew :zygarde-test:dependencies > deps/zygarde-test.txt
./gradlew :zygarde-web:dependencies > deps/zygarde-web.txt
./gradlew :zygarde-webmvc:dependencies > deps/zygarde-webmvc.txt
./gradlew :zygarde-webmvc-codegen:dependencies > deps/zygarde-webmvc-codegen.txt
./gradlew :zygarde-webmvc-security:dependencies > deps/zygarde-webmvc-security.txt
