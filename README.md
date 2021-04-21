# TIS Trainee Sync

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

This project uses [Semantic Versioning](https://semver.org).

## Operational Support

### Force a data refresh

The model/dto packages describe the entities that are received by the single API endpoint.  Each of this can be reloaded in full:
1. Agree a time to trigger the load, as doing so will increase the latency in normal data migration.
2. Log into the web console
3. Go to the "Database Migration Service" 
4. Select the task describing the source and target
5. Go to "Table Statistics" page
6. Select the Entity Type(s) to be refreshed and press "Reload table data"

## License

This project is license under [The MIT License (MIT)](LICENSE).