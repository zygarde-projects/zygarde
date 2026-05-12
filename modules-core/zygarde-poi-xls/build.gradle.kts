apply(plugin = "io.spring.dependency-management")
dependencies {
  api("org.apache.poi:poi-ooxml:5.5.1")
  testImplementation(project(":zygarde-test"))
}
