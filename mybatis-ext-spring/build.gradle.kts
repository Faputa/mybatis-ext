dependencies {
    api(project(":mybatis-ext"))
    api("org.mybatis:mybatis-spring")
    compileOnly("org.mybatis:mybatis")
    compileOnly("org.springframework:spring-context")
    compileOnly("org.springframework:spring-jdbc")
    testImplementation(project(":mybatis-ext-test"))
    testImplementation("org.springframework:spring-test")
    testImplementation("com.zaxxer:HikariCP")
}
