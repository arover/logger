plugins {
    application
//    id("com.github.johnrengelman.shadow") version "6.0.0"
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

dependencies {
    implementation(files("libs/bcprov-jdk18on-171.jar"))
//    implementation(fileTree(dir: 'libs', include: ['*.jar'])
}
application {
    mainClass.set("com.arover.logger.cmdtool.LogCmdTool")
}

