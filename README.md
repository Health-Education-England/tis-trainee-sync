# TIS Trainee Sync

[![Build Status][build-badge]][build-href]
[![License][license-badge]][license-href]
[![Quality Gate Status][quality-gate-badge]][quality-gate-href]
[![Coverage Stats][coverage-badge]][coverage-href]

## About

This service handles synchronization of data from TIS Core to TIS Self Service.

### Placement Enrichment Flow

To synchronise a placement it must be enriched with additional data from several associated data
types. When an update is received for one of these data types all associated placements must be
updated and synchronised to reflect the changes.

This is achieved by following the associations to find all placements needing an update and then
queuing them to be processed as a placement load. Where the association between the modified record
and the placement is indirect, such as trust > post > placement, then an intermediate queue is also
used, in this case a `Post` queue is added.

![placement enrichment flow](placement-enrich-flow.svg)

## Operational Support

### Force a data refresh

The model/dto packages describe the entities that are received by the single API endpoint. Each of
this can be reloaded in full:

1. Agree a time to trigger the load, as doing so will increase the latency in normal data migration.
2. Log into the web console
3. Go to the "Database Migration Service"
4. Select the task describing the source and target
5. Go to "Table Statistics" page
6. Select the Entity Type(s) to be refreshed and press "Reload table data"

## Developing

### Running

```shell
gradlew bootRun
```

#### Pre-Requisites

- A MongoDB instance.
- Access to queues as listed below. This can be achieved via an instance of [localstack](https://localstack.cloud/) with the SQS queues set up, or access to AWS SQS queues.

#### Environmental Variables

| Name                          | Description                                                          | Default   |
|-------------------------------|----------------------------------------------------------------------|-----------|
| **Database:**                 |                                                                      |           |
| DB_HOST                       | The MongoDB host to connect to.                                      | localhost |
| DB_PORT                       | The port to connect to MongoDB on.                                   | 27017     |
| DB_NAME                       | The name of the MongoDB database.                                    | sync      |
| DB_USER                       | The username to access the MongoDB instance.                         | admin     |
| DB_PASSWORD                   | The password to access the MongoDB instance.                         | pwd       |
| AUTH_SOURCE                   | The authentication database.                                         | admin     |
| **Logging:**                  |                                                                      |           |
| SENTRY_DSN                    | A Sentry error monitoring Data Source Name.                          |           |
| SENTRY_ENVIRONMENT            | The environment to log Sentry events against.                        | local     |
| LOGGING_ROOT                  | Root logging level.                                                  | INFO      |
| LOGGING_EVENT                 | Sync event logging level.                                            | DEBUG     |
| LOGGING_SERVICE               | Sync service logging level.                                          | DEBUG     |
| **Queues:**                   |                                                                      |           |
| PLACEMENT_QUEUE_URL           | Queue from which to receive placements.                              |           |
| PLACEMENT_SPECIALTY_QUEUE_URL | Queue from which to receive placement specialties.                   |           |
| PROFILE_CREATED_QUEUE_URL     | Queue from which to receive profile creation notifications.          |           |
| POST_QUEUE_URL                | Queue from which to receive posts.                                   |           |
| RECORD_QUEUE_URL              | Queue from which to receive general data records of different types. |           |
| REQUEST_QUEUE_URL             | Queue to which to post requests for data.                            |           |
| **Related services:**         |                                                                      |           |
| REFERENCE_HOST                | Reference service host.                                              | localhost |
| REFERENCE_PORT                | Reference service port.                                              | 8205      |
| TRAINEE_DETAILS_HOST          | Trainee details service host.                                        | localhost |
| TRAINEE_DETAILS_PORT          | Trainee details service port.                                        | 8203      |


#### Usage Examples

## Usage

Spring Actuator is included to provide health check and info endpoints, which can be accessed
at `<host>:<port>/sync/actuator/health` and
`<host>:<port>/sync/actuator/info` respectively.

### Testing

The Gradle `test` task can be used to run automated tests and produce coverage
reports.
```shell
gradlew test
```

The Gradle `check` lifecycle task can be used to run automated tests and also
verify formatting conforms to the code style guidelines.
```shell
gradlew check
```

### Building

```shell
gradlew bootBuildImage
```

## Versioning

This project uses [Semantic Versioning](https://semver.org).

## License

This project is licensed under [The MIT License (MIT)](LICENSE).

[coverage-badge]: https://sonarcloud.io/api/project_badges/measure?project=Health-Education-England_tis-trainee-sync&metric=coverage

[coverage-href]: https://sonarcloud.io/component_measures?metric=coverage&id=Health-Education-England_tis-trainee-sync

[build-badge]: https://badgen.net/github/checks/health-education-england/tis-trainee-sync?label=build&icon=github

[build-href]: https://github.com/Health-Education-England/tis-trainee-sync/actions/workflows/ci-cd-workflow.yml

[license-badge]: https://badgen.net/github/license/health-education-england/tis-trainee-sync

[license-href]: LICENSE

[quality-gate-badge]: https://sonarcloud.io/api/project_badges/measure?project=Health-Education-England_tis-trainee-sync&metric=alert_status

[quality-gate-href]: https://sonarcloud.io/summary/new_code?id=Health-Education-England_tis-trainee-sync
