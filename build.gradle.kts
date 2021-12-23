import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "1.5.10"
    application
    `maven-publish`
}

group = "net.ksoftware.kdockfx"
version = "1.0"

repositories {
    mavenCentral()
}

dependencies {
    compileOnly("org.openjfx:javafx-controls:17.0.0.1:win")
    compileOnly("org.openjfx:javafx-graphics:17.0.0.1:win")
    compileOnly("org.openjfx:javafx-base:17.0.0.1:win")
    testImplementation(kotlin("test"))
}


tasks.test {
    useJUnitPlatform()
}

tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "11"
}

tasks.withType<JavaCompile> {
    options.release.set(11)
}

application {
    mainClass.set("kdockfx.MainKt")
}

publishing {
    publications {
        create<MavenPublication>("kdockfx") {
            groupId = group.toString()
            artifactId = "kdockfx"
            version = "1.0"

            from(components["java"])
        }
    }
}