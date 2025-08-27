plugins {
  java
  alias(libs.plugins.spring.boot)
  alias(libs.plugins.spring.dependency.management)

  // Code quality plugins
  checkstyle
  jacoco
  alias(libs.plugins.sonarqube)
}

group = "uk.nhs.hee.tis.trainee"
version = "1.21.1"

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
    mavenBom(libs.spring.cloud.dependencies.aws.get().toString())
    mavenBom(libs.spring.cloud.dependencies.core.get().toString())
  }
}

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
  implementation(libs.mapstruct.core)
  annotationProcessor(libs.mapstruct.processor)
  testAnnotationProcessor(libs.mapstruct.processor)

  // Sentry reporting
  implementation(libs.sentry.core)

  // Required to support PATCH requests.
  implementation("org.apache.httpcomponents.client5:httpclient5")

  // Amazon
  implementation("io.awspring.cloud:spring-cloud-aws-starter-sqs")
  implementation("io.awspring.cloud:spring-cloud-aws-starter-sns")

  implementation("org.springframework.cloud:spring-cloud-starter-bootstrap")

  implementation(libs.bundles.mongock)

  testImplementation("org.springframework.cloud:spring-cloud-starter")
  testImplementation("org.springframework.cloud:spring-cloud-contract-wiremock")
  testImplementation("org.testcontainers:testcontainers")
  testImplementation("org.testcontainers:junit-jupiter")

  val playtikaTestContainersVersion = "3.1.15"
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
