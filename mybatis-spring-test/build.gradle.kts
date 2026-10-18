dependencies {
    implementation("org.mybatis.spring.boot:mybatis-spring-boot-starter")
    testImplementation(project(":mybatis-ext-test"))
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("com.alibaba.fastjson2:fastjson2:2.0.53")
}
