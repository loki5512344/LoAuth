import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

plugins {
    id("com.gradleup.shadow")
}

dependencies {
    implementation(project(":common"))
    implementation(project(":discord")) {
        exclude(group = "ch.qos.logback")
    }
    compileOnly("com.velocitypowered:velocity-api:3.5.0-SNAPSHOT")
    annotationProcessor("com.velocitypowered:velocity-api:3.5.0-SNAPSHOT")
}


tasks.named<ShadowJar>("shadowJar") {
    archiveBaseName.set("loauth")
    archiveClassifier.set("")
}