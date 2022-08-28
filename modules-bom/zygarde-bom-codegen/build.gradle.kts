apply(plugin = "java-platform")

group = if (group == "com.github.zygarde-projects") {
  "com.github.zygarde-projects.zygarde"
} else {
  group
}

dependencies {
  constraints {
    "api"("com.squareup:kotlinpoet:1.9.0")
    "api"("com.squareup:kotlinpoet-metadata:1.9.0")
    "api"("com.squareup:kotlinpoet-metadata-specs:1.9.0")
    "api"("com.google.auto.service:auto-service:1.0")
    "api"("io.github.classgraph:classgraph:4.8.21")
  }
}
