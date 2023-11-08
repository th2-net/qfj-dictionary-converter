plugins {
    kotlin("jvm") version "1.9.0"
    application
    id("com.gorylenko.gradle-git-properties") version "2.4.1"
}

group = "com.exactpro.th2"
version = project.property("release_version") as String

dependencies {
    implementation(platform(kotlin("bom")))
    implementation("com.github.ajalt.clikt:clikt:4.2.1")
    implementation("com.github.ajalt.mordant:mordant:2.2.0")
    implementation("com.exactpro.sf:service-fix:3.3.149")
    testImplementation(kotlin("test"))
}

tasks.test {
    useJUnitPlatform()
}

kotlin {
    jvmToolchain(11)
}

application {
    mainClass.set("com.exactpro.th2.converter.cli.Main")
}

distributions {
    main {
        contents {
            from("README.md")
        }
    }
}