dependencies {
  implementation("com.squareup:kotlinpoet:1.9.0")
  implementation("com.squareup:kotlinpoet-metadata:1.9.0")
}

tasks.getByName("jar").enabled = true
tasks.getByName("printCoverage").enabled = false
