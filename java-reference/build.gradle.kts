plugins {
    `java-library`
    application
}

group = "org.mapsforgekit"
version = "0.1.0-SNAPSHOT"

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(17))
    }
}

repositories {
    mavenCentral()
}

dependencies {
    // Original Mapsforge — used to produce reference outputs for Swift tests.
    // Pin to a specific version so reference outputs are deterministic.
    implementation("org.mapsforge:mapsforge-core:0.21.0")
    implementation("org.mapsforge:mapsforge-map:0.21.0")
    implementation("org.mapsforge:mapsforge-map-reader:0.21.0")
    implementation("org.mapsforge:mapsforge-themes:0.21.0")

    // JSON output for cross-language test fixtures
    implementation("com.fasterxml.jackson.core:jackson-databind:2.17.2")

    // Testing
    testImplementation(platform("org.junit:junit-bom:5.11.0"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    testImplementation("com.flipkart.zjsonpatch:zjsonpatch:0.4.16")
}

application {
    // CLI entry point that dumps a tile from a .map file as JSON.
    // Usage: ./gradlew run --args="path/to/file.map z x y"
    mainClass.set("org.mapsforgekit.reference.ReferenceDumpCli")
}

tasks.test {
    useJUnitPlatform()
    testLogging {
        events("passed", "skipped", "failed")
    }
}
