plugins {
    `kotlin-dsl`
}

repositories {
    google()
    mavenCentral()
}

dependencies {
    // YAML parser for reading rule files
    implementation("org.yaml:snakeyaml:2.6") // NOSONAR kotlin:S6624
}

