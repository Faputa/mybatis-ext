dependencies {
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    testImplementation(project(":mybatis-ext-test"))
    testImplementation("org.springframework.boot:spring-boot-starter-test")
}
