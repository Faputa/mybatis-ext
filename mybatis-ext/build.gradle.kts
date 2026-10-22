plugins { id("com.gradleup.shadow") }

val shaded by configurations.creating

configurations.compileOnly { extendsFrom(shaded) }

dependencies {
    shaded(platform(project(":")))
    compileOnly("org.mybatis:mybatis")
    api("com.google.code.findbugs:jsr305")
    shaded("org.javassist:javassist")
    testImplementation("org.apache.commons:commons-dbcp2")
    // 导入的 Boot BOM 排除了 DBCP 的日志依赖，此处保留原 Maven 测试运行时所需的依赖。
    testRuntimeOnly("commons-logging:commons-logging:1.2")
}

tasks.jar { archiveClassifier = "plain" }

tasks.shadowJar {
    archiveClassifier = ""
    configurations = listOf(shaded)
    relocate("javassist", "io.github.mybatisext.javassist")
}

// 保留项目依赖以支持 IDE 源码关联，运行时使用发布产物。
listOf("apiElements", "runtimeElements").forEach { name ->
    configurations.named(name) {
        outgoing.artifacts.clear()
        outgoing.artifact(tasks.shadowJar)
    }
}

tasks.assemble { dependsOn(tasks.shadowJar) }
