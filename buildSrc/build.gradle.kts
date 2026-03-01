plugins {
    `kotlin-dsl`
}

repositories {
    google()
    mavenCentral()
}

dependencies {
    // YAML parser for reading rule files
    implementation("org.yaml:snakeyaml:2.2") // NOSONAR kotlin:S6624
}

