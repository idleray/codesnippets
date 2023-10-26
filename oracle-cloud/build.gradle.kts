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
    implementation(platform("com.oracle.oci.sdk:oci-java-sdk-bom:3.26.0"))
    implementation("com.oracle.oci.sdk:oci-java-sdk-core")
    implementation("com.oracle.oci.sdk:oci-java-sdk-common-httpclient-jersey")
    implementation("com.oracle.oci.sdk:oci-java-sdk-identity")
    implementation("com.typesafe:config:1.4.1")

    // SLF4J dependencies
    implementation ("org.slf4j:slf4j-api:1.7.32")
    implementation ("org.slf4j:slf4j-simple:1.7.32")


    testImplementation(kotlin("test"))
}

tasks.test {
    useJUnitPlatform()
}

kotlin {
    jvmToolchain(11)
}

application {
    mainClass.set("MainKt")
}