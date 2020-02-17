import java.net.URI
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    // Apply the Kotlin JVM plugin to add support for Kotlin.
    id("org.jetbrains.kotlin.jvm") version "1.3.61"

    // Serialization
    id("org.jetbrains.kotlin.plugin.serialization") version "1.3.61" apply true

    // Apply the application plugin to add support for building a CLI application.
    application
}

repositories {
    mavenCentral()
    jcenter()
    maven {
        url = URI("https://jcenter.bintray.com")
    }
    maven {
        url = URI("https://oss.sonatype.org/content/repositories/snapshots/")
    }
    maven {
        url = URI("https://dl.bintray.com/micronaut/core-releases-local")
    }
}

buildscript {
    dependencies {
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:1.3.61")
        classpath("org.jetbrains.kotlin:kotlin-serialization:1.3.61")
    }
}

tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "1.8"
}

dependencies {
    // Align versions of all Kotlin components
    implementation(platform("org.jetbrains.kotlin:kotlin-bom"))

    // Use the Kotlin JDK 8 standard library.
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")

    // Use the Kotlin test library.
    testImplementation("org.jetbrains.kotlin:kotlin-test")

    // Use the Kotlin JUnit integration.
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit")

    implementation("io.github.zeroone3010:yetanotherhueapi:1.2.0")

    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.3.3")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-jdk8:1.3.3")

    // Serialization
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-runtime:0.14.0")

    // Fuel
    implementation("com.github.kittinunf.fuel:fuel:2.2.1")

    // PicoCLI
    implementation("info.picocli:picocli:4.2.0")
    annotationProcessor("info.picocli:picocli-codegen:4.2.0")
}

application {
    // Define the main class for the application.
    mainClassName = "com.example.AppKt"
}

// Read from STDIN
val run by tasks.getting(JavaExec::class) {
    standardInput = System.`in`
}