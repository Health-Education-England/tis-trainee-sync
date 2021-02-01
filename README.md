# TIS Microservice Template

## About
This service handles synchronization of data from TIS Core to TIS Self Service.

## Usage
The service can be started using the `bootRun` Gradle task with the included
Gradle wrapper `./gradlew bootRun`.

Spring Actuator is included to provide health check  and info endpoints, which
can be accessed at `<host>:<port>/sync/actuator/health` and
`<host>:<port>/sync/actuator/info` respectively.

## Development environment

### Associated services
The tis-trainee-sync service interacts with the tis-trainee-reference and 
tis-trainee-details components, as described in the application.yml file. These 
need to be running locally for integration testing.

You will also need permissions on the (staging / pre-production) AWS SQS queue. 

(What about AWS Kinesis?)

### End-to-end testing
You can test the tis-trainee-sync service by POSTing JSON data to
[http://local.tis-selfservice.com/sync/api/record]. 

For example, using curl:

`curl --header "Content-Type: application/json"  --request POST --data-binary \ 
"@data.json" http://local.tis-selfservice.com/sync/api/record`

where data.json comprises data and metadata objects, each containing an array of 
values. 

The metadata object should contain the following attributes:
 - timestamp
 - record-type = 'data' (or...?)
 - operation = 'insert' / 'load' / 'update' (these are functionally equivalent) 
 (what about 'delete'?)
 - partition-key-type = 'primary-key' (any other option?)
 - schema-name = 'tcs' (or 'reference'?)
 - table-name = table name from TCS (or REFERENCE?) MySQL database 
 
 The data object should contain the following attributes:
- id = object id (this is mapped to TraineeDetailsDto.tisId or ReferenceDto.tisId)
- other relevant attributes from TraineeDetailsDto or ReferenceDto depending on the object 
class. The name of an attribute may differ between the JSON object and the DTO: the 
TraineeDetailsMapper / ReferenceMapper interfaces define the translation from one name 
to another. In particular, note that personId and traineeId are used synonymously in 
different TCS tables.
- what about foreign keys e.g. siteId, postId?

Example data.json:
`{
"data":	{
"id":	11210,
"programmeMembershipType":	"SUBSTANTIVE",
"curriculumStartDate":	"2009-08-05",
"curriculumEndDate":	"2012-08-02",
"programmeStartDate":	"2009-08-05",
"curriculumCompletionDate":	"2012-08-02",
"programmeEndDate":	"2012-08-02",
"programmeId":	18009,
"curriculumId":	128,
"trainingNumberId":	5498,
"intrepidId":	"269154142",
"personId":	47165,
"amendedDate":	"2018-05-08T00:00:00Z"
},
"metadata":	{
"timestamp":	"2021-01-14T11:50:17.133721Z",
"record-type":	"data",
"operation":	"load",
"partition-key-type":	"primary-key",
"schema-name":	"tcs",
"table-name":	"ProgrammeMembership"
}
}`

(How can one source this sort of test data, if not composing it manually?)

To inspect the persistence of the new data in the TIS Self Service MongoDB instance, 
you will need tis-trainee-details running locally.

## Deployment
 - Provide `SENTRY_DSN` and `SENTRY_ENVIRONMENT` as environmental variables
   during deployment.

## Versioning
This project uses [Semantic Versioning](semver.org).

## License
This project is license under [The MIT License (MIT)](LICENSE).


[http://local.tis-selfservice.com/sync/api/record]: (local.tis-selfservice.com/sync/api/record)
