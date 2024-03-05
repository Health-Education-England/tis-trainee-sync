FROM openjdk:17

EXPOSE 8208

COPY ./build/libs/tis-trainee-sync-1.9.0.jar app.jar

ENV AWS_REGION=""
ENV REDIS_HOST=redis-cache
ENV PLACEMENT_QUEUE_URL=http://localstack:4566/queue/tis-trainee-sync-local-placement
ENV REDIS_USER=default
ENV TRAINEE_DETAILS_HOST=tis-trainee-details
ENV UPDATE_GMC_DETAILS_TOPIC_ARN=arn:aws:sns:eu-west-2:000000000000:tis-trainee-sync-update-gmc-details-event
ENV PLACEMENT_SPECIALTY_QUEUE_URL=http://localstack:4566/queue/tis-trainee-sync-local-placement-specialty
ENV RECORD_QUEUE_URL=http://localstack:4566/queue/tis-trainee-sync-local-record
ENV DELETE_PLACEMENT_TOPIC_ARN=arn:aws:sns:eu-west-2:000000000000:tis-trainee-sync-delete-placement-event
ENV REQUEST_QUEUE_URL=http://localstack:4566/queue/tis-trainee-sync-local-data-request
ENV REDIS_PASSWORD=password
ENV UPDATE_PERSON_TOPIC_ARN=arn:aws:sns:eu-west-2:000000000000:tis-trainee-sync-update-person-event
ENV UPDATE_PLACEMENT_TOPIC_ARN=arn:aws:sns:eu-west-2:000000000000:tis-trainee-sync-update-placement-event
ENV AWS_SECRET_ACCESS_KEY=""
ENV CURRICULUM_MEMBERSHIP_QUEUE_URL=http://localstack:4566/queue/tis-trainee-sync-local-curriculum-membership
ENV DELETE_PROGRAMME_MEMBERSHIP_TOPIC_ARN=arn:aws:sns:eu-west-2:000000000000:tis-trainee-sync-delete-programme-membership-event
ENV AWS_ACCESS_KEY_ID=""
ENV UPDATE_PROGRAMME_MEMBERSHIP_TOPIC_ARN=arn:aws:sns:eu-west-2:000000000000:tis-trainee-sync-update-programme-membership-event
ENV POST_SPECIALTY_QUEUE_URL=http://localstack:4566/queue/tis-trainee-sync-local-post-specialty
ENV UPDATE_CONTACT_DETAILS_TOPIC_ARN=arn:aws:sns:eu-west-2:000000000000:tis-trainee-sync-update-contact-details-event
ENV PROFILE_CREATED_QUEUE_URL=http://localstack:4566/queue/tis-trainee-sync-local-profile-created
ENV UPDATE_PERSONAL_INFO_TOPIC_ARN=arn:aws:sns:eu-west-2:000000000000:tis-trainee-sync-update-personal-info-event
ENV UPDATE_GDC_DETAILS_TOPIC_ARN=arn:aws:sns:eu-west-2:000000000000:tis-trainee-sync-update-gdc-details-event
ENV POST_QUEUE_URL=http://localstack:4566/queue/tis-trainee-sync-local-post
ENV DB_HOST=host.docker.internal
ENV UPDATE_PERSON_OWNER_TOPIC_ARN=arn:aws:sns:eu-west-2:000000000000:tis-trainee-sync-update-person-owner-event
ENV PROGRAMME_MEMBERSHIP_QUEUE_URL=http://localstack:4566/queue/tis-trainee-sync-local-programme-membership
ENV REFERENCE_HOST=tis-trainee-reference

CMD ["java", "-jar", "app.jar"]
