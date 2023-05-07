
pluginManagement {
    repositories {
        maven("https://maven.aliyun.com/repository/central") { isAllowInsecureProtocol = true }
        maven("https://maven.aliyun.com/repository/google") { isAllowInsecureProtocol = true }
        maven("https://maven.aliyun.com/repository/public") { isAllowInsecureProtocol = true }
        maven("https://maven.aliyun.com/repository/gradle-plugin") {
            isAllowInsecureProtocol = true
        }
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        maven("https://maven.aliyun.com/repository/central") { isAllowInsecureProtocol = true }
        maven("https://maven.aliyun.com/repository/google") { isAllowInsecureProtocol = true }
        maven("https://maven.aliyun.com/repository/public") { isAllowInsecureProtocol = true }
        google() { isAllowInsecureProtocol = true }
        mavenCentral() { isAllowInsecureProtocol = true }
    }
}

rootProject.name = "Logger"
include(":app", ":log"
    , ":cmdtool"
)

