import org.gradle.api.artifacts.component.ModuleComponentIdentifier
import org.gradle.api.publish.maven.MavenPublication

plugins {
    base
    `java-platform`
    `maven-publish`
    signing
    id("com.diffplug.spotless") version "8.2.1"
    id("com.gradleup.shadow") version "9.2.2" apply false
    id("org.jetbrains.intellij.platform") version "2.19.0" apply false
}

group = "io.github.mybatis-ext"

spotless {
    java {
        target(subprojects.map { "${it.name}/src/**/*.java" })
        // 与 VS Code Java 默认分组一致，末尾的点用于限定包名边界。
        importOrder("#", "java.", "javax.", "org.", "com.", "")
        eclipse("4.37").configFile(".vscode/java-formatter.xml").apply {
            providers.gradleProperty("eclipseMirror").orNull?.let {
                withP2Mirrors(mapOf("https://download.eclipse.org" to it))
            }
        }
        trimTrailingWhitespace()
        endWithNewline()
    }
    kotlinGradle {
        target("*.gradle.kts", "*/build.gradle.kts")
        ktfmt("0.61").kotlinlangStyle()
    }
}

javaPlatform { allowDependencies() }

dependencies {
    api(platform("org.springframework.boot:spring-boot-dependencies:2.7.18"))
    api(platform("org.mybatis.spring.boot:mybatis-spring-boot:2.3.1"))
    constraints {
        listOf("mybatis-ext", "mybatis-ext-spring", "mybatis-ext-spring-boot-starter").forEach {
            api("${project.group}:$it:${project.version}")
        }
        api("org.apache.commons:commons-dbcp2:2.9.0")
        api("com.h2database:h2:2.2.220")
        api("com.google.code.findbugs:jsr305:3.0.2")
        api("org.javassist:javassist:3.30.2-GA")
    }
}

allprojects {
    repositories {
        // 复用已下载的第三方依赖，排除本地可能过时的核心模块产物。
        mavenLocal { content { excludeGroup("io.github.mybatis-ext") } }
        mavenCentral()
    }
    tasks.withType<JavaCompile>().configureEach { options.encoding = "UTF-8" }
    tasks.withType<AbstractArchiveTask>().configureEach {
        isPreserveFileTimestamps = false
        isReproducibleFileOrder = true
    }
}

val publishedModules = setOf("mybatis-ext", "mybatis-ext-spring", "mybatis-ext-spring-boot-starter")

subprojects {
    group = rootProject.group
    version = rootProject.version
    if (name != "mybatis-ext-idea-plugin") {
        apply(plugin = "java-library")
        extensions.configure<JavaPluginExtension> {
            toolchain { languageVersion = JavaLanguageVersion.of(17) }
            sourceCompatibility = JavaVersion.VERSION_1_8
            targetCompatibility = JavaVersion.VERSION_1_8
            withSourcesJar()
            withJavadocJar()
        }
        tasks.withType<JavaCompile>().configureEach { options.release = 8 }
        tasks.withType<Jar>().configureEach {
            from(rootProject.file("LICENSE")) { into("META-INF") }
        }
        tasks.withType<Javadoc>().configureEach {
            (options as StandardJavadocDocletOptions).apply {
                encoding = "UTF-8"
                addStringOption("Xdoclint:none", "-quiet")
            }
        }
        configurations.named("testImplementation") {
            extendsFrom(configurations.getByName("compileOnly"))
        }
        dependencies {
            add("api", platform(rootProject))
            add("testImplementation", "org.junit.jupiter:junit-jupiter-api")
            add("testImplementation", "com.h2database:h2")
            add("testRuntimeOnly", "org.junit.jupiter:junit-jupiter-engine")
            add("testRuntimeOnly", "org.junit.platform:junit-platform-launcher")
        }
        tasks.withType<Test>().configureEach {
            useJUnitPlatform()
            systemProperty("file.encoding", "UTF-8")
            maxHeapSize = "1g"
            testLogging { events("failed", "skipped") }
        }
        if (name in publishedModules) {
            apply(plugin = "maven-publish")
            apply(plugin = "signing")
            extensions.configure<PublishingExtension> {
                publications.create<MavenPublication>("mavenJava") {
                    from(components["java"])
                    // 将 Gradle 的 compileOnly 映射为 Maven 的 provided，不作为运行时依赖。
                    pom.withXml {
                        val provided = configurations.getByName("compileOnly").dependencies
                        if (provided.isNotEmpty()) {
                            val root = asNode()
                            val node =
                                (root.get("dependencies") as groovy.util.NodeList).firstOrNull()
                                    as? groovy.util.Node ?: root.appendNode("dependencies")
                            val resolved =
                                configurations
                                    .getByName("compileClasspath")
                                    .incoming
                                    .resolutionResult
                                    .allComponents
                            provided.forEach { dependency ->
                                val id =
                                    resolved
                                        .mapNotNull { it.id as? ModuleComponentIdentifier }
                                        .first {
                                            it.group == dependency.group &&
                                                it.module == dependency.name
                                        }
                                node.appendNode("dependency").apply {
                                    appendNode("groupId", id.group)
                                    appendNode("artifactId", id.module)
                                    appendNode("version", id.version)
                                    appendNode("scope", "provided")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

publishing {
    publications.create<MavenPublication>("mavenBom") { from(components["javaPlatform"]) }
}

configure(listOf(project) + subprojects.filter { it.name in publishedModules }) {
    extensions.configure<PublishingExtension> {
        publications.withType<MavenPublication>().configureEach {
            pom {
                name = project.name
                description = "MyBatis-Ext enhancements for MyBatis"
                url = "https://github.com/Faputa/mybatis-ext"
                licenses {
                    license {
                        name = "The MIT License (MIT)"
                        url = "https://opensource.org/licenses/MIT"
                    }
                }
                developers {
                    developer {
                        id = "Faputa"
                        name = "Faputa"
                        email = "2238932729@qq.com"
                    }
                }
                scm {
                    url = "https://github.com/Faputa/mybatis-ext"
                    connection = "scm:git:https://github.com/Faputa/mybatis-ext.git"
                    developerConnection = "scm:git:https://github.com/Faputa/mybatis-ext.git"
                }
            }
        }
        repositories {
            maven {
                name = "buildRepository"
                url = rootProject.layout.buildDirectory.dir("repository").get().asFile.toURI()
            }
        }
    }
    if (providers.gradleProperty("signPublications").orNull == "true") {
        extensions.configure<SigningExtension> {
            useGpgCmd()
            sign(extensions.getByType<PublishingExtension>().publications)
        }
    }
}

tasks.named("check") { dependsOn(subprojects.map { "${it.path}:check" }) }

tasks.named("clean") { dependsOn(subprojects.map { "${it.path}:clean" }) }

tasks.named("assemble") { dependsOn(subprojects.map { "${it.path}:assemble" }) }

tasks.named("publish") { dependsOn(publishedModules.map { ":$it:publish" }) }

tasks.named("publishToMavenLocal") {
    dependsOn(publishedModules.map { ":$it:publishToMavenLocal" })
}

tasks.register("verifyAll") {
    group = "verification"
    description =
        "Test all modules, verify the IDEA plugin, build its ZIP and generate Maven publication metadata."
    dependsOn(
        "check",
        ":mybatis-ext-idea-plugin:buildPlugin",
        ":mybatis-ext-idea-plugin:verifyPluginStructure",
        ":mybatis-ext-idea-plugin:verifyPlugin",
    )
    dependsOn(tasks.withType<GenerateMavenPom>())
    dependsOn(
        subprojects
            .filter { it.name in publishedModules }
            .map { "${it.path}:generatePomFileForMavenJavaPublication" }
    )
}

tasks.register<Zip>("releaseBundle") {
    group = "distribution"
    description =
        "Verify and collect the current Maven publications, plugin and release documentation without uploading."
    dependsOn("verifyAll", "publish")
    archiveFileName = "mybatis-ext-${project.version}-release.zip"
    destinationDirectory = layout.buildDirectory.dir("distributions")
    from(layout.buildDirectory.dir("repository")) {
        include(
            (publishedModules + rootProject.name).map {
                "io/github/mybatis-ext/$it/${project.version}/**"
            }
        )
        into("maven")
    }
    from(project(":mybatis-ext-idea-plugin").layout.buildDirectory.dir("distributions")) {
        include("mybatis-ext-idea-plugin-${project(":mybatis-ext-idea-plugin").version}.zip")
        into("idea")
    }
    from("docs") { into("docs") }
    from("README.md", "LICENSE")
}
