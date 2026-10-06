// Standalone fixture compiled with Kotlin 2.3 to reproduce the intersection-override metadata regression
// (KotlinReflectionInternalError from KProperty.javaField). Regenerate the committed jars with ./regenerate.sh.
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
  kotlin("jvm") version "2.3.21"
}

repositories {
  mavenCentral()
}

kotlin {
  // keep the stdlib API surface compatible with Zygarde's Kotlin 2.2.20 test runtime
  coreLibrariesVersion = "2.2.20"
  compilerOptions {
    jvmTarget.set(JvmTarget.JVM_17)
  }
}

java {
  sourceCompatibility = JavaVersion.VERSION_17
  targetCompatibility = JavaVersion.VERSION_17
}
