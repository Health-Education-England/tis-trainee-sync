/*
 * The MIT License (MIT)
 *
 * Copyright 2023 Crown Copyright (Health Education England)
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and
 * associated documentation files (the "Software"), to deal in the Software without restriction,
 * including without limitation the rights to use, copy, modify, merge, publish, distribute,
 * sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or
 * substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT
 * NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM,
 * DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package uk.nhs.hee.tis.trainee.sync.config;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import uk.nhs.hee.tis.trainee.sync.config.EcsMetadataConfiguration.EcsMetadata;
import uk.nhs.hee.tis.trainee.sync.config.EcsMetadataConfiguration.EcsMetadata.ContainerMetadata;
import uk.nhs.hee.tis.trainee.sync.config.EcsMetadataConfiguration.EcsMetadata.ContainerMetadata.LogOptions;
import uk.nhs.hee.tis.trainee.sync.config.EcsMetadataConfiguration.EcsMetadata.TaskMetadata;

class EcsMetadataConfigurationTest {

  private static final String ECS_METADATA_ENDPOINT = "https://ecs.metadata/endpoint";

  private EcsMetadataConfiguration configuration;

  @BeforeEach
  void setUp() {
    configuration = new EcsMetadataConfiguration();
  }

  @Test
  void shouldReturnEcsMetadataWhenEndpointFound() {
    RestTemplate restTemplate = mock(RestTemplate.class);
    when(restTemplate.getForObject(ECS_METADATA_ENDPOINT, ContainerMetadata.class)).thenReturn(
        new ContainerMetadata("containerArn", new LogOptions("logGroup", "region", "logStream")));
    when(restTemplate.getForObject(ECS_METADATA_ENDPOINT + "/task", TaskMetadata.class)).thenReturn(
        new TaskMetadata("cluster", "taskArn", "family", "revision"));

    EcsMetadata ecsMetadata = configuration.ecsMetadata(restTemplate, ECS_METADATA_ENDPOINT);

    assertThat("Unexpected ECS metadata.", ecsMetadata, notNullValue());

    TaskMetadata taskMetadata = ecsMetadata.taskMetadata();
    assertThat("Unexpected task metadata.", taskMetadata, notNullValue());
    assertThat("Unexpected cluster.", taskMetadata.cluster(), is("cluster"));
    assertThat("Unexpected task ARN.", taskMetadata.taskArn(), is("taskArn"));
    assertThat("Unexpected family.", taskMetadata.family(), is("family"));

    ContainerMetadata containerMetadata = ecsMetadata.containerMetadata();
    assertThat("Unexpected container metadata.", containerMetadata, notNullValue());
    assertThat("Unexpected container ARN.", containerMetadata.containerArn(), is("containerArn"));

    LogOptions logOptions = containerMetadata.logOptions();
    assertThat("Unexpected task metadata.", logOptions, notNullValue());
    assertThat("Unexpected log group.", logOptions.logGroup(), is("logGroup"));
    assertThat("Unexpected log region.", logOptions.region(), is("region"));
    assertThat("Unexpected log stream.", logOptions.logStream(), is("logStream"));
  }

  @Test
  void shouldThrowExceptionWhenMetadataEndpointNotFound() {
    RestTemplate restTemplate = mock(RestTemplate.class);
    when(restTemplate.getForObject(any(String.class), any())).thenThrow(RestClientException.class);

    assertThrows(RestClientException.class,
        () -> configuration.ecsMetadata(restTemplate, ECS_METADATA_ENDPOINT));
  }
}
