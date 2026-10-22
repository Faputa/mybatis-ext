import java.util.Properties
import org.jetbrains.intellij.platform.gradle.IntelliJPlatformType
import org.jetbrains.intellij.platform.gradle.TestFrameworkType
import org.jetbrains.intellij.platform.gradle.tasks.VerifyPluginTask

plugins {
    java
    id("org.jetbrains.intellij.platform")
}

group = "io.github.mybatis-ext"

repositories { intellijPlatform { defaultRepositories() } }

val localProperties =
    Properties().apply {
        val source = file("local.properties")
        if (source.exists()) source.inputStream().use { load(it) }
    }
val ideaHome =
    providers
        .gradleProperty("ideaHome")
        .orElse(providers.environmentVariable("IDEA_HOME"))
        .orElse(provider { localProperties.getProperty("ideaHome") })

dependencies {
    implementation(platform(project(":")))
    implementation(project(":mybatis-ext"))
    implementation("org.mybatis:mybatis")
    testImplementation("junit:junit:4.13.2")
    intellijPlatform {
        intellijIdeaCommunity("2023.3.8") { useInstaller = false }
        bundledPlugin("com.intellij.java")
        testFramework(TestFrameworkType.Platform)
        testFramework(TestFrameworkType.Plugin.Java)
        pluginVerifier("1.411")
    }
}

java { toolchain { languageVersion = JavaLanguageVersion.of(17) } }

tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
    options.release = 17
}

tasks.jar {
    from("../LICENSE") {
        into("META-INF")
        rename { "LICENSE-mybatis-ext.txt" }
    }
}

intellijPlatform {
    pluginConfiguration {
        id = "io.github.mybatisext.idea"
        name = "MyBatis Ext"
        version = project.version.toString()
        ideaVersion {
            sinceBuild = "233"
            untilBuild = "262.*"
        }
        vendor { name = "mybatis-ext" }
    }
    buildSearchableOptions = false
    pluginVerification {
        ides {
            current()
            providers
                .gradleProperty("verificationIdeVersions")
                .orNull
                ?.split(',')
                ?.filter { it.isNotBlank() }
                ?.forEach {
                    create(IntelliJPlatformType.IntellijIdeaCommunity, it.trim()) {
                        useInstaller = false
                    }
                }
            providers
                .gradleProperty("verificationIdePaths")
                .orNull
                ?.split(';')
                ?.filter { it.isNotBlank() }
                ?.forEach { local(it) }
        }
    }
}

if (ideaHome.isPresent) {
    intellijPlatformTesting {
        runIde.register("runLocalIde") { localPath = ideaHome.map { file(it) } }
        testIde.register("testLocalIde") {
            localPath = ideaHome.map { file(it) }
            testFrameworks(TestFrameworkType.Platform, TestFrameworkType.Plugin.Java)
        }
    }
    tasks.named<Test>("testLocalIde") {
        dependsOn(tasks.processTestResources)
        classpath += files(sourceSets.test.get().output.resourcesDir)
    }
}

tasks.named<VerifyPluginTask>("verifyPlugin") {
    // 验证器运行于独立 JVM，需要显式转发命令行指定的下载代理。
    listOf("http.proxyHost", "http.proxyPort", "https.proxyHost", "https.proxyPort").forEach { key
        ->
        providers.systemProperty(key).orNull?.let { systemProperty(key, it) }
    }
}

tasks.withType<Test>().configureEach {
    dependsOn(":mybatis-ext:shadowJar")
    maxHeapSize = "2g"
    systemProperty("java.awt.headless", "true")
    // 仅运行当前插件及其声明的 Java 和平台依赖。
    systemProperty("idea.load.plugins.id", "io.github.mybatisext.idea,com.intellij.java")
    // 核心类同时用作 PSI 测试素材；适配器不加载应用类。
    doFirst {
        systemProperty(
            "mybatisext.core.jar",
            configurations.runtimeClasspath
                .get()
                .first { it.name == "mybatis-ext-${rootProject.version}.jar" }
                .absolutePath,
        )
        systemProperty(
            "mybatisext.mybatis.jar",
            configurations.runtimeClasspath
                .get()
                .first { it.name.startsWith("mybatis-3.") }
                .absolutePath,
        )
    }
}

tasks.processTestResources {
    from("src/test/java/io/github/mybatisext/idea/fixtures") { into("fixtures") }
}
