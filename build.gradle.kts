plugins {
  java
  id("org.springframework.boot") version "3.3.2"
  id("io.spring.dependency-management") version "1.1.6"

  // Code quality plugins
  checkstyle
  jacoco
  id("org.sonarqube") version "5.1.0.4882"
}

group = "uk.nhs.hee.tis.trainee"
version = "1.19.1"

configurations {
  compileOnly {
    extendsFrom(configurations.annotationProcessor.get())
  }
}

repositories {
  mavenCentral()
}

dependencyManagement {
  imports {
    mavenBom("io.awspring.cloud:spring-cloud-aws-dependencies:3.1.1")
    mavenBom("org.springframework.cloud:spring-cloud-dependencies:2023.0.3")
  }
}

val mongockVersion = "5.4.2"

dependencies {
  // Spring Boot starters
  implementation("org.springframework.boot:spring-boot-starter-data-mongodb")
  implementation("org.springframework.boot:spring-boot-starter-actuator")
  implementation("org.springframework.boot:spring-boot-starter-validation")
  implementation("org.springframework.boot:spring-boot-starter-web")
  implementation("org.springframework.boot:spring-boot-starter-cache")
  implementation("org.springframework.boot:spring-boot-starter-data-redis")
  testImplementation("org.springframework.boot:spring-boot-starter-test")

  // Lombok
  compileOnly("org.projectlombok:lombok")
  annotationProcessor("org.projectlombok:lombok")

  // MapStruct
  val mapstructVersion = "1.5.5.Final"
  implementation("org.mapstruct:mapstruct:${mapstructVersion}")
  annotationProcessor("org.mapstruct:mapstruct-processor:${mapstructVersion}")
  testAnnotationProcessor("org.mapstruct:mapstruct-processor:${mapstructVersion}")

  // Sentry reporting
  implementation("io.sentry:sentry-spring-boot-starter-jakarta:7.13.0")

  // Required to support PATCH requests.
  implementation("org.apache.httpcomponents.client5:httpclient5")

  // Amazon
  implementation("io.awspring.cloud:spring-cloud-aws-starter-sqs")
  implementation("io.awspring.cloud:spring-cloud-aws-starter-sns")

  implementation("org.springframework.cloud:spring-cloud-starter-bootstrap")

  implementation("io.mongock:mongock-springboot:${mongockVersion}")
  implementation("io.mongock:mongodb-springdata-v4-driver:${mongockVersion}")

  val testContainersVersion = "1.19.8"
  testImplementation("org.springframework.cloud:spring-cloud-starter")
  testImplementation("org.springframework.cloud:spring-cloud-contract-wiremock")
  testImplementation("org.testcontainers:testcontainers:${testContainersVersion}")
  testImplementation("org.testcontainers:junit-jupiter:${testContainersVersion}")

  val playtikaTestContainersVersion = "3.1.7"
  testImplementation("com.playtika.testcontainers:embedded-redis:${playtikaTestContainersVersion}")
  testImplementation("com.playtika.testcontainers:embedded-mongodb:${playtikaTestContainersVersion}")
}

checkstyle {
  config = resources.text.fromArchiveEntry(configurations.checkstyle.get().first(), "google_checks.xml")
}

java {
  toolchain {
    languageVersion.set(JavaLanguageVersion.of(17))
    vendor.set(JvmVendorSpec.ADOPTIUM)
  }
}

sonarqube {
  properties {
    property("sonar.host.url", "https://sonarcloud.io")
    property("sonar.login", System.getenv("SONAR_TOKEN"))
    property("sonar.organization", "health-education-england")
    property("sonar.projectKey", "Health-Education-England_tis-trainee-sync")

    property("sonar.java.checkstyle.reportPaths",
      "build/reports/checkstyle/main.xml,build/reports/checkstyle/test.xml")
  }
}

tasks.jacocoTestReport {
  reports {
    html.required.set(true)
    xml.required.set(true)
  }
}

tasks.test {
  finalizedBy(tasks.jacocoTestReport)
  useJUnitPlatform()
}
