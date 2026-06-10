plugins {
    java
    id("com.gradleup.shadow") version "9.3.1" apply false
}

allprojects {
    group = "dev.loki"
    version = "0.0.1"

    repositories {
        mavenCentral()
        maven {
            name = "papermc"
            url = uri("https://repo.papermc.io/repository/maven-public/")
        }
    }
}

subprojects {
    apply(plugin = "java")

    java {
        toolchain.languageVersion.set(JavaLanguageVersion.of(21))
    }
}
