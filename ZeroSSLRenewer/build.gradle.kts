plugins {
    kotlin("jvm") version "1.9.0"
    application
}

group = "com.rayject"
version = "1.0-SNAPSHOT"

repositories {
    maven ( url = "https://maven.aliyun.com/repository/central" )
    maven ( url = "https://maven.aliyun.com/repository/gradle-plugin" )
    maven ( url = "https://maven.aliyun.com/repository/google" )
}

dependencies {
    implementation(platform("com.squareup.okhttp3:okhttp-bom:4.10.0"))
    implementation("com.squareup.okhttp3:okhttp")
    implementation("com.squareup.okhttp3:logging-interceptor")
    implementation("org.bouncycastle:bcpkix-jdk15on:1.70")
    implementation("com.google.code.gson:gson:2.10.1")

    testImplementation(kotlin("test"))
}

tasks.test {
    useJUnitPlatform()
}

kotlin {
    jvmToolchain(8)
}

application {
    mainClass.set("MainKt")
}