apply(plugin = "io.spring.dependency-management")
dependencies {
  api("org.apache.poi:poi-ooxml:5.2.2")
  testImplementation(project(":zygarde-test"))
}

tasks.getByName("printCoverage").enabled = false
