# TIS Microservice Template

## About
This service handles synchronization of data from TIS Core to TIS Self Service.

## Usage
The service can be started using the `bootRun` Gradle task with the included
Gradle wrapper `./gradlew bootRun`.

Spring Actuator is included to provide health check  and info endpoints, which
can be accessed at `<host>:<port>/sync/actuator/health` and
`<host>:<port>/sync/actuator/info` respectively.

## Deployment
 - Provide `SENTRY_DSN` and `SENTRY_ENVIRONMENT` as environmental variables
   during deployment.

## Versioning
This project uses [Semantic Versioning](semver.org).

## License
This project is license under [The MIT License (MIT)](LICENSE).
