apply(plugin = "java-platform")

dependencies {
  constraints {
    "api"("com.squareup:kotlinpoet:1.18.1")
    "api"("com.squareup:kotlinpoet-metadata:1.18.1")
    "api"("com.google.auto.service:auto-service:1.1.1")
    "api"("io.github.classgraph:classgraph:4.8.184")
  }
}
