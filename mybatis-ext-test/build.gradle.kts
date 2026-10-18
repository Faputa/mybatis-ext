dependencies {
    api(project(":mybatis-ext"))
    api("org.mybatis:mybatis")
    api("ch.qos.logback:logback-classic")
    testImplementation("org.springframework:spring-jdbc")
}
