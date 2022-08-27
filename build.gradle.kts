buildscript {
  repositories {
    mavenCentral()
    maven("https://plugins.gradle.org/m2/")
    maven("https://repo.spring.io/plugins-release")
  }
}

plugins {
  id("org.jlleitschuh.gradle.ktlint") version "10.2.0"
  id("org.jetbrains.dokka") version "1.6.10"
  id("io.gitlab.arturbosch.detekt") version "1.18.1"
  id("de.jansauer.printcoverage") version "2.0.0"
  id("org.springframework.boot") version "2.6.3"
  id("io.spring.dependency-management") version "1.0.11.RELEASE"
  kotlin("jvm") version "1.6.10"
  kotlin("plugin.spring") version "1.6.10"
  kotlin("kapt") version "1.6.10"
  `maven-publish`
  jacoco
  application
}

fun Project.isBomProject() = this.name.startsWith("zygarde-bom")
fun Project.isPublishingProject() = this.name.startsWith("zygarde")

allprojects {
  group = "zygarde"

  if (!isBomProject()) {
    apply(plugin = "org.jlleitschuh.gradle.ktlint")
  }

  repositories {
    mavenCentral()
    maven("https://jitpack.io")
  }
}

subprojects {
  if (isPublishingProject()) {
    apply(plugin = "org.gradle.maven-publish")
    apply(plugin = "org.jetbrains.dokka")
    apply(plugin = "io.gitlab.arturbosch.detekt")

    publishing {
      repositories {
        maven {
          name = "Nexus"
          url = uri("https://nexus.puni.tw/repository/maven-releases")
          credentials {
            username = System.getenv("PUNI_NEXUS_DEPLOY_USER")
            password = System.getenv("PUNI_NEXUS_DEPLOY_PWD")
          }
        }
        maven {
          name = "Github"
          url = uri("https://maven.pkg.github.com/zygarde-projects/zygarde")
          credentials {
            username = System.getenv("ZYGARDE_GH_USER") ?: System.getenv("GITHUB_ACTOR")
            password = System.getenv("ZYGARDE_GH_TOKEN") ?: System.getenv("GITHUB_TOKEN")
          }
        }
      }
    }
  }

  if (isBomProject()) {
    apply(plugin = "java-platform")
    publishing {
      publications {
        create<MavenPublication>("default") {
          from(components["javaPlatform"])
        }
      }
    }
    return@subprojects
  }

  apply(plugin = "de.jansauer.printcoverage")
  apply(plugin = "io.spring.dependency-management")
  apply(plugin = "kotlin")
  apply(plugin = "kotlin-kapt")
  apply(plugin = "org.jetbrains.kotlin.jvm")
  apply(plugin = "org.gradle.jacoco")

  configure<io.spring.gradle.dependencymanagement.dsl.DependencyManagementExtension> {
    imports {
      mavenBom("org.springframework.cloud:spring-cloud-dependencies:2021.0.0")
    }
  }

  configure<JavaPluginExtension> {
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
  }

  tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
    kotlinOptions {
      freeCompilerArgs = listOf("-Xjsr305=strict")
      jvmTarget = "1.8"
    }
  }

  dependencies {
    implementation("org.jetbrains.kotlin:kotlin-reflect")
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit5")
    testImplementation("io.kotest:kotest-assertions-shared-jvm:4.6.3")
    testImplementation("io.kotest:kotest-assertions-core-jvm:4.6.3")
    testImplementation("io.mockk:mockk:1.12.0")
  }

  configurations.all {
    resolutionStrategy {
      eachDependency {
        when (requested.module.name) {
          "kotlinx-coroutines-core" -> useVersion("1.5.1")
          "kotlinx-coroutines-jdk8" -> useVersion("1.5.1")
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
  tasks.getByName("jacocoTestReport").finalizedBy("printCoverage")

  tasks.withType<Test> {
    useJUnitPlatform()
  }

  jacoco {
    toolVersion = "0.8.7"
  }

  tasks.withType<JacocoReport> {
    reports {
      html.required.set(true)
      xml.required.set(true)
      csv.required.set(false)
    }
  }

  if (isPublishingProject()) {
    val dokkaJavadoc by tasks.getting(org.jetbrains.dokka.gradle.DokkaTask::class)

    val dokkaJar by tasks.creating(Jar::class) {
      group = JavaBasePlugin.DOCUMENTATION_GROUP
      description = "Assembles Kotlin docs with Dokka"
      archiveClassifier.set("javadoc")
      from(dokkaJavadoc.outputDirectory)
    }

    val sourceJar by tasks.creating(Jar::class) {
      group = JavaBasePlugin.DOCUMENTATION_GROUP
      description = "Source"
      archiveClassifier.set("sources")
      from(sourceSets.getByName("main").allSource)
    }

    tasks.detekt {
      detekt {
        source = files("src/*/kotlin")
      }
    }

    publishing {
      publications {
        create<MavenPublication>("default") {
          from(components["java"])
          artifact(sourceJar)
          artifact(dokkaJar)

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
}

task("lint") {
  dependsOn("ktlintFormat")
}
task("lintc") {
  dependsOn("ktlintCheck")
}

tasks.getByName("publish").enabled = false
tasks.getByName("printCoverage").enabled = false
tasks.getByName("bootJar").enabled = false

task("collectJacocoSourcePath", Exec::class) {
  val paths = subProjectsForJacoco
    .flatMap { it.sourceSets.getByName("main").allJava.srcDirs }
    .joinToString(" ")
  commandLine = listOf("echo", paths)
}
