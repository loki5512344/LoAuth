plugins {
    java
    id("com.gradleup.shadow") version "9.3.1" apply false
    checkstyle
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
    apply(plugin = "checkstyle")

    java {
        toolchain.languageVersion.set(JavaLanguageVersion.of(21))
    }

    checkstyle {
        toolVersion = "10.21.4"
        configFile = rootProject.file("config/checkstyle/checkstyle.xml")
        isIgnoreFailures = false
        maxErrors = 0
        maxWarnings = 0
    }

    tasks.withType<Checkstyle>().configureEach {
        reports {
            xml.required.set(true)
            html.required.set(true)
        }
    }
}
