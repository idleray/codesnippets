pluginManagement {
    repositories {
        maven ( url = "https://maven.aliyun.com/repository/central" )
        maven ( url = "https://maven.aliyun.com/repository/gradle-plugin" )
        maven ( url = "https://maven.aliyun.com/repository/google" )
    }
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.5.0"
}

rootProject.name = "oracle-cloud"