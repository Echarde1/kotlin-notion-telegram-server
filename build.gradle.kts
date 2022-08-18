import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

val ktor_version: String by project
val kotlin_version: String by project
val logback_version: String by project
val notion_sdk_version: String by project
val telegram_bot_sdk_version: String by project
val jackson_version: String by project
val kotlinx_serialization_version: String by project
val dotenv_version: String by project
val arrow_version: String by project

plugins {
    kotlin("jvm") version "1.7.0"
    kotlin("plugin.serialization") version "1.7.0"
    id("com.google.devtools.ksp") version "1.7.0-1.0.6"
    application
}

group = "me.user"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
    maven { url = uri("https://jitpack.io") }
    maven {
        url = uri("https://maven.pkg.jetbrains.space/public/p/ktor/eap")
    }
    maven {
        url = uri("https://maven.pkg.jetbrains.space/kotlin/p/kotlin/dev")
    }
    maven {
        url = uri("https://s01.oss.sonatype.org/content/repositories/releases/")
    }
}

dependencies {
    implementation("io.ktor:ktor-client-core:$ktor_version")
    implementation("io.ktor:ktor-client-cio:$ktor_version")
    implementation("io.ktor:ktor-server-netty:$ktor_version")
    implementation("io.ktor:ktor-client-logging:$ktor_version")
//    implementation("io.ktor:ktor-server-core-jvm:$ktor_version")
//    implementation("io.ktor:ktor-server-content-negotiation:2.0.3")
//    implementation("io.ktor:ktor-serialization-kotlinx-xml:2.0.3")
//    implementation("io.ktor:ktor-serialization-kotlinx-json:2.0.3")

    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:$kotlinx_serialization_version")

    implementation("org.jetbrains.kotlin:kotlin-reflect:1.7.0")

    implementation("ch.qos.logback:logback-classic:$logback_version")
    implementation("io.github.kotlin-telegram-bot.kotlin-telegram-bot:telegram:$telegram_bot_sdk_version")

    implementation("com.fasterxml.jackson.dataformat:jackson-dataformat-xml:$jackson_version")
    implementation("com.fasterxml.jackson.module:jackson-module-parameter-names:$jackson_version")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:$jackson_version")
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310:$jackson_version")
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jdk8:$jackson_version")

    implementation("io.arrow-kt:arrow-core:$arrow_version")
    implementation("io.arrow-kt:arrow-optics:$arrow_version")
    implementation("io.arrow-kt:arrow-fx-coroutines:$arrow_version")
    ksp("io.arrow-kt:arrow-optics-ksp-plugin:$arrow_version")

    implementation("org.jraf:klibnotion:$notion_sdk_version")

    implementation("io.github.cdimascio:dotenv-kotlin:$dotenv_version")

    testImplementation(kotlin("test"))
    testImplementation("io.ktor:ktor-server-tests:$ktor_version")
    // Не может определить версию этой либы с котлином 1.7.0
//    testImplementation("org.jetbrains.kotlin:kotlin-test-junit:$kotlin_version")
}

tasks.test {
    useJUnitPlatform()
}

tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "1.8"
}

tasks.withType(KotlinCompile::class).all {
    kotlinOptions.freeCompilerArgs = listOf("-Xcontext-receivers")
}

application {
    mainClass.set("MainKt")
}

tasks {
    create("stage").dependsOn("installDist")
}