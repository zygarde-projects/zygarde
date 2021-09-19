buildscript {
  repositories {
    jcenter()
    mavenCentral()
    maven("https://plugins.gradle.org/m2/")
    maven("https://repo.spring.io/plugins-release")
  }
}

plugins {
  id("org.jlleitschuh.gradle.ktlint") version "9.3.0"
  id("org.jetbrains.dokka") version "0.10.1"
  id("io.gitlab.arturbosch.detekt") version "1.3.1"
  id("de.jansauer.printcoverage") version "2.0.0"
  id("org.springframework.boot") version "2.3.1.RELEASE"
  id("io.spring.dependency-management") version "1.0.8.RELEASE"
  kotlin("jvm") version "1.5.31"
  kotlin("plugin.spring") version "1.5.31"
  kotlin("kapt") version "1.5.31"
  `maven-publish`
  jacoco
  application
}

allprojects {
  group = "puni"
  apply(plugin = "org.jlleitschuh.gradle.ktlint")

  repositories {
    mavenCentral()
    jcenter()
    maven("https://jitpack.io")
  }

  ktlint {
    enableExperimentalRules.set(true)
    version.set("0.38.0")
  }
}

subprojects {
  apply(plugin = "org.gradle.maven-publish")

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
    }
  }

  if (name.startsWith("zygarde-bom")) {
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
  apply(plugin = "org.jetbrains.dokka")
  apply(plugin = "io.gitlab.arturbosch.detekt")
  apply(plugin = "org.gradle.jacoco")

  configure<io.spring.gradle.dependencymanagement.dsl.DependencyManagementExtension> {
    imports {
      mavenBom("org.springframework.cloud:spring-cloud-dependencies:Hoxton.RELEASE")
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
    testImplementation("io.kotest:kotest-assertions-shared-jvm:4.2.0")
    testImplementation("io.kotest:kotest-assertions-core-jvm:4.2.0")
    testImplementation("io.mockk:mockk:1.9.3")
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
      html.isEnabled = true
      xml.isEnabled = true
      csv.isEnabled = false
    }
  }

  tasks.dokka {
    outputFormat = "html"
    outputDirectory = "$buildDir/javadoc"
  }

  val dokkaJar by tasks.creating(Jar::class) {
    group = JavaBasePlugin.DOCUMENTATION_GROUP
    description = "Assembles Kotlin docs with Dokka"
    archiveClassifier.set("javadoc")
    from(tasks.dokka)
  }

  val sourceJar by tasks.creating(Jar::class) {
    group = JavaBasePlugin.DOCUMENTATION_GROUP
    description = "Source"
    archiveClassifier.set("sources")
    from(sourceSets.getByName("main").allSource)
  }

  tasks.detekt {
    detekt {
      input = files("src/*/kotlin")
    }
  }

  publishing {
    publications {
      create<MavenPublication>("default") {
        from(components["java"])
        artifact(sourceJar)
        artifact(dokkaJar)
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
    html.isEnabled = true
    xml.isEnabled = true
  }

  dependsOn(
    *subProjectsForJacoco.map { it.tasks.getByName("test") }.toTypedArray()
  )
}

task("lint") {
  dependsOn("ktlintFormat")
}

tasks.getByName("publish").enabled = false
tasks.getByName("printCoverage").enabled = false
tasks.getByName("bootJar").enabled = false

tasks.dokka {
  outputFormat = "html"
  outputDirectory = "$buildDir/javadoc"
  subProjects = subprojects.map { it.name }
  configuration {
    moduleName = "doc"
  }
}

task("collectJacocoSourcePath", Exec::class) {
  val paths = subProjectsForJacoco
    .flatMap { it.sourceSets.getByName("main").allJava.srcDirs }
    .joinToString(" ")
  commandLine = listOf("echo", paths)
}
