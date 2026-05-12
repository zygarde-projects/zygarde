buildscript {
  repositories {
    mavenCentral()
    maven("https://plugins.gradle.org/m2/")
    maven("https://repo.spring.io/plugins-release")
  }
}

plugins {
  id("org.jlleitschuh.gradle.ktlint") version "12.1.2"
  id("io.gitlab.arturbosch.detekt") version "1.23.8"
  id("org.springframework.boot") version "3.5.14"
  id("io.spring.dependency-management") version "1.1.7"
  id("com.google.devtools.ksp") version "1.9.25-1.0.20" apply false
  kotlin("jvm") version "1.9.25"
  kotlin("plugin.spring") version "1.9.25"
  kotlin("kapt") version "1.9.25"
  `maven-publish`
  signing
  jacoco
  application
}

val stagingDir = layout.buildDirectory.dir("staging-deploy")

tasks.register<Zip>("zipStagingRepository") {
  group = "publishing"
  description = "Creates a bundle zip for Maven Central upload"
  archiveFileName.set("bundle.zip")
  destinationDirectory.set(layout.buildDirectory.dir("distributions"))
  from(stagingDir)

  // Ensure all subproject publish tasks complete before zipping
  dependsOn(subprojects.mapNotNull { it.tasks.findByName("publishAllPublicationsToLocalStagingRepository") })
}

fun Project.isBomProject() = this.name.startsWith("zygarde-bom")

fun Project.isPublishingProject() = this.name.startsWith("zygarde")

allprojects {
  if (!isBomProject()) {
    apply(plugin = "org.jlleitschuh.gradle.ktlint")
  }

  repositories {
    mavenCentral()
    maven("https://jitpack.io")
  }
}

subprojects {
  version = rootProject.version

  if (isPublishingProject()) {
    apply(plugin = "org.gradle.maven-publish")
    apply(plugin = "org.gradle.signing")
    apply(plugin = "io.gitlab.arturbosch.detekt")

    publishing {
      repositories {
        maven {
          name = "LocalStaging"
          url = uri(rootProject.layout.buildDirectory.dir("staging-deploy"))
        }
      }
    }
  }

  if (isBomProject()) {
    apply(plugin = "java-platform")
    apply(plugin = "org.gradle.signing")
    publishing {
      publications {
        create<MavenPublication>("default") {
          groupId = "io.github.zygarde-projects"
          from(components["javaPlatform"])

          pom {
            name.set(project.name)
            description.set("Zygarde BOM - Bill of Materials for Zygarde framework")
            url.set("https://github.com/zygarde-projects/zygarde")

            licenses {
              license {
                name.set("MIT License")
                url.set("https://opensource.org/licenses/MIT")
              }
            }

            developers {
              developer {
                id.set("puni")
                name.set("puni")
                url.set("https://github.com/puni")
              }
            }

            scm {
              url.set("https://github.com/zygarde-projects/zygarde")
              connection.set("scm:git:git://github.com/zygarde-projects/zygarde.git")
              developerConnection.set("scm:git:ssh://git@github.com/zygarde-projects/zygarde.git")
            }
          }
        }
      }
    }
    signing {
      val signingKey = System.getenv("GPG_SIGNING_KEY")
      val signingPassword = System.getenv("GPG_SIGNING_PASSWORD") ?: ""
      if (!signingKey.isNullOrBlank()) {
        useInMemoryPgpKeys(signingKey, signingPassword)
        sign(publishing.publications["default"])
      }
    }
    return@subprojects
  }

  apply(plugin = "io.spring.dependency-management")
  apply(plugin = "kotlin")
  apply(plugin = "kotlin-kapt")
  apply(plugin = "org.jetbrains.kotlin.jvm")
  apply(plugin = "org.gradle.jacoco")

  configure<io.spring.gradle.dependencymanagement.dsl.DependencyManagementExtension> {
    imports {
      mavenBom("org.springframework.cloud:spring-cloud-dependencies:2025.0.2")
    }
  }

  configure<JavaPluginExtension> {
    toolchain {
      languageVersion.set(JavaLanguageVersion.of(21))
    }
    sourceCompatibility = JavaVersion.VERSION_21
    targetCompatibility = JavaVersion.VERSION_21
  }

  tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
    kotlinOptions {
      jvmTarget = "21"
    }
  }

  dependencies {
    implementation("org.jetbrains.kotlin:kotlin-reflect")
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit5")
    testImplementation("io.kotest:kotest-assertions-shared-jvm:5.9.1")
    testImplementation("io.kotest:kotest-assertions-core-jvm:5.9.1")
    testImplementation("io.mockk:mockk:1.13.17")
  }

  configurations.all {
    resolutionStrategy {
      eachDependency {
        when (requested.module.name) {
          "kotlinx-coroutines-core" -> useVersion("1.8.0")
          "kotlinx-coroutines-jdk8" -> useVersion("1.8.0")
        }
      }
    }
    exclude(group = "junit")
    exclude(group = "org.junit.vintage", module = "junit-vintage-engine")
  }

  task("housekeeping", Delete::class) {
    delete(file("out"))
  }

  tasks.getByName("clean").finalizedBy("housekeeping")
  tasks.getByName("test").finalizedBy("jacocoTestReport")

  tasks.withType<Test> {
    useJUnitPlatform()
  }

  jacoco {
    toolVersion = "0.8.14"
  }

  tasks.withType<JacocoReport> {
    reports {
      html.required.set(true)
      xml.required.set(true)
      csv.required.set(false)
    }
  }

  tasks.jar {
    archiveClassifier.set("")
  }

  if (isPublishingProject()) {
    val sourceJar by tasks.creating(Jar::class) {
      group = JavaBasePlugin.DOCUMENTATION_GROUP
      description = "Source"
      archiveClassifier.set("sources")
      from(sourceSets.getByName("main").allSource)
    }

    val javadocJar by tasks.creating(Jar::class) {
      group = JavaBasePlugin.DOCUMENTATION_GROUP
      description = "Javadoc"
      archiveClassifier.set("javadoc")
      from(tasks.named("javadoc"))
    }

    tasks.detekt {
      detekt {
        source = files("src/*/kotlin")
      }
    }

    publishing {
      publications {
        create<MavenPublication>("default") {
          groupId = "io.github.zygarde-projects"
          from(components["java"])
          artifact(sourceJar)
          artifact(javadocJar)

          pom {
            name.set(project.name)
            description.set("Zygarde - A Kotlin framework for simplifying enterprise application development")
            url.set("https://github.com/zygarde-projects/zygarde")

            licenses {
              license {
                name.set("MIT License")
                url.set("https://opensource.org/licenses/MIT")
              }
            }

            developers {
              developer {
                id.set("puni")
                name.set("puni")
                url.set("https://github.com/puni")
              }
            }

            scm {
              url.set("https://github.com/zygarde-projects/zygarde")
              connection.set("scm:git:git://github.com/zygarde-projects/zygarde.git")
              developerConnection.set("scm:git:ssh://git@github.com/zygarde-projects/zygarde.git")
            }
          }

          // XXX merge dependencyMangement in generated pom.xml
          // https://github.com/spring-gradle-plugins/dependency-management-plugin/issues/257
          pom.withXml {
            val root = asNode()
            val nodes = root["dependencyManagement"] as groovy.util.NodeList
            if (nodes.size > 1) {
              val lastDependencyManagement = nodes.last() as groovy.util.Node
              val lastNodeDependencies = (lastDependencyManagement.get("dependencies") as groovy.util.NodeList).get(0) as groovy.util.Node
              nodes.take(nodes.size - 1).forEach { n ->
                if (n is groovy.util.Node) {
                  val dependencies = (n.get("dependencies") as groovy.util.NodeList).getAt("dependency")
                  dependencies.forEach { d ->
                    val dNode = d as groovy.util.Node
                    lastNodeDependencies.append(dNode)
                  }
                  root.remove(n)
                }
              }
            }
          }
        }
      }
    }

    signing {
      val signingKey = System.getenv("GPG_SIGNING_KEY")
      val signingPassword = System.getenv("GPG_SIGNING_PASSWORD") ?: ""
      if (!signingKey.isNullOrBlank()) {
        useInMemoryPgpKeys(signingKey, signingPassword)
        sign(publishing.publications["default"])
      }
    }
  }
}

val jacocoIgnoreProjects = listOf(
  "zygarde-bom-codegen",
  "zygarde-bom-codegen-test"
)
val subProjectsForJacoco = subprojects.filterNot {
  it.name in jacocoIgnoreProjects
}

task("covAll", JacocoReport::class) {
  executionData(
    fileTree(rootDir.absolutePath).include(
      *subProjectsForJacoco
        .map { "${it.name}/build/jacoco/*.exec" }
        .toTypedArray()
    )
  )
  sourceSets(
    *subProjectsForJacoco
      .map {
        it.sourceSets.getByName("main")
      }
      .toTypedArray()
  )
  reports {
    html.required.set(true)
    xml.required.set(true)
  }

  dependsOn(
    *subProjectsForJacoco.map { it.tasks.getByName("test") }.toTypedArray()
  )

  task("lint") {
    dependsOn("ktlintFormat")
  }
}

task("lintc") {
  dependsOn("ktlintCheck")
}

tasks.getByName("publish").enabled = false
tasks.getByName("bootJar").enabled = false
tasks.getByName("jar").enabled = false

task("collectJacocoSourcePath", Exec::class) {
  val paths = subProjectsForJacoco
    .flatMap { it.sourceSets.getByName("main").allJava.srcDirs }
    .joinToString(" ")
  commandLine = listOf("echo", paths)
}
