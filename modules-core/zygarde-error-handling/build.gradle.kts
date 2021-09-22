apply(plugin = "org.springframework.boot")
apply(plugin = "org.jetbrains.kotlin.plugin.spring")

dependencies {
  api(project(":zygarde-core-extensions"))
}

tasks.getByName("printCoverage").enabled = false
tasks.getByName("bootJar").enabled = false
tasks.getByName("jar").enabled = true
