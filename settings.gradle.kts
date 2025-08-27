rootProject.name = "tis-trainee-sync"

plugins {
  id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

dependencyResolutionManagement {
  repositories {
    mavenCentral()
  }

  versionCatalogs {
    create("libs") {
      from("uk.nhs.tis.trainee:version-catalog:0.0.8")
    }
  }
}
