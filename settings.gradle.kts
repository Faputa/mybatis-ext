pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
    }
}

rootProject.name = "mybatis-ext-parent"

include(
    "mybatis-ext",
    "mybatis-ext-test",
    "mybatis-ext-spring",
    "mybatis-ext-spring-boot-starter",
    "mybatis-flex-test",
    "mybatis-plus-test",
    "mybatis-spring-test",
    "spring-data-jpa-test",
    "mybatis-ext-idea-plugin",
)
