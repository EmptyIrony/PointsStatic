import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import io.izzel.taboolib.gradle.*
import io.izzel.taboolib.gradle.Basic
import io.izzel.taboolib.gradle.Bukkit
import io.izzel.taboolib.gradle.BukkitUtil
import io.izzel.taboolib.gradle.BukkitUI
import io.izzel.taboolib.gradle.BukkitHook
import io.izzel.taboolib.gradle.Database


plugins {
    java
    id("io.izzel.taboolib") version "2.0.22"
    id("org.jetbrains.kotlin.jvm") version "1.8.22"
}

taboolib {
    env {
        install(Basic)
        install(Bukkit)
        install(BukkitUtil)
        install(BukkitUI)
        install(BukkitHook)
        install(Database)
        install(CommandHelper)
    }
    description {
        name = "PointsStatic"
        contributors {
            name("存在")
        }
    }
    version { taboolib = "6.2.3" }
}

repositories {
    mavenCentral()
}

dependencies {
    compileOnly("com.google.guava:guava:30.1-jre")
    compileOnly("ink.ptms.core:v12004:12004:mapped")
    compileOnly("ink.ptms.core:v12004:12004:universal")
    compileOnly(kotlin("stdlib"))
    compileOnly(fileTree("libs"))
}

tasks.withType<JavaCompile> {
    options.encoding = "UTF-8"
}

tasks.withType<KotlinCompile> {
    kotlinOptions {
        jvmTarget = "1.8"
        freeCompilerArgs = listOf("-Xjvm-default=all")
    }
}

configure<JavaPluginConvention> {
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
}
