dependencies {
    implementation("com.baomidou:mybatis-plus-boot-starter:3.5.4")
    implementation("com.baomidou:dynamic-datasource-spring-boot-starter:3.5.0")
    implementation(project(":mybatis-ext-spring-boot-starter"))
    testImplementation(project(":mybatis-ext-test"))
    testImplementation("org.springframework.boot:spring-boot-starter-test")
}
