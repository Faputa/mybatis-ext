dependencies {
    api(project(":mybatis-ext-spring"))
    api("org.mybatis.spring.boot:mybatis-spring-boot-starter")
    annotationProcessor(platform(rootProject))
    annotationProcessor("org.springframework.boot:spring-boot-configuration-processor")
    testImplementation(project(":mybatis-ext-test"))
    testImplementation("org.springframework.boot:spring-boot-starter-test")
}
