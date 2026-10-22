dependencies {
    implementation("com.mybatis-flex:mybatis-flex-spring-boot-starter:1.7.3")
    annotationProcessor("com.mybatis-flex:mybatis-flex-processor:1.7.3")
    testAnnotationProcessor("com.mybatis-flex:mybatis-flex-processor:1.7.3")
    implementation(project(":mybatis-ext-spring-boot-starter"))
    testImplementation(project(":mybatis-ext-test"))
    testImplementation("org.springframework.boot:spring-boot-starter-test")
}
