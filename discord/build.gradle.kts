import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

plugins {
    id("com.gradleup.shadow")
}

dependencies {
    implementation(project(":common"))
    implementation("net.dv8tion:JDA:6.4.0")
    implementation("org.slf4j:slf4j-api:2.0.17")
    runtimeOnly("ch.qos.logback:logback-classic:1.5.31")
}

tasks.named<ShadowJar>("shadowJar") {
    archiveBaseName.set("discord")
    archiveClassifier.set("standalone")

    manifest {
        attributes["Main-Class"] = "dev.loki.loAuth.discord.BotLauncher"
    }
}
