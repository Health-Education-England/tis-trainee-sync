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

package uk.nhs.hee.tis.trainee.sync.facade;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

import com.amazonaws.services.sns.AmazonSNS;
import com.amazonaws.services.sqs.AmazonSQSAsync;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.awspring.cloud.autoconfigure.messaging.SqsAutoConfiguration;
import io.awspring.cloud.messaging.core.QueueMessagingTemplate;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.client.RestTemplate;
import org.testcontainers.junit.jupiter.Testcontainers;
import uk.nhs.hee.tis.trainee.sync.dto.TraineeDetailsDto;
import uk.nhs.hee.tis.trainee.sync.event.CurriculumEventListener;
import uk.nhs.hee.tis.trainee.sync.event.CurriculumMembershipEventListener;
import uk.nhs.hee.tis.trainee.sync.event.ProgrammeEventListener;
import uk.nhs.hee.tis.trainee.sync.event.ProgrammeMembershipEventListener;
import uk.nhs.hee.tis.trainee.sync.model.Curriculum;
import uk.nhs.hee.tis.trainee.sync.model.CurriculumMembership;
import uk.nhs.hee.tis.trainee.sync.model.Person;
import uk.nhs.hee.tis.trainee.sync.model.Programme;
import uk.nhs.hee.tis.trainee.sync.model.ProgrammeMembership;
import uk.nhs.hee.tis.trainee.sync.repository.CurriculumMembershipRepository;
import uk.nhs.hee.tis.trainee.sync.repository.CurriculumRepository;
import uk.nhs.hee.tis.trainee.sync.repository.PersonRepository;
import uk.nhs.hee.tis.trainee.sync.repository.ProgrammeMembershipRepository;
import uk.nhs.hee.tis.trainee.sync.repository.ProgrammeRepository;

@SpringBootTest(properties = "embedded.mongodb.enabled=true")
@ActiveProfiles("int")
@Testcontainers(disabledWithoutDocker = true)
@EnableAutoConfiguration(exclude = SqsAutoConfiguration.class)
class ProgrammeMembershipEnrichmentIntegrationTest {

  private static final String TRAINEE_ID = String.valueOf(new Random().nextLong());

  private static final String CURRICULUM_ID = UUID.randomUUID().toString();
  private static final String CURRICULUM_NAME = "Dermatology";
  private static final String CURRICULUM_SUB_TYPE = "MEDICAL_CURRICULUM";

  private static final String CURRICULUM_MEMBERSHIP_ID = UUID.randomUUID().toString();
  private static final LocalDate CURRICULUM_MEMBERSHIP_START_DATE = LocalDate.now().minusYears(1L);
  private static final LocalDate CURRICULUM_MEMBERSHIP_END_DATE = LocalDate.now().plusYears(1L);

  private static final String PROGRAMME_ID = String.valueOf(new Random().nextLong());
  private static final String PROGRAMME_NAME = UUID.randomUUID().toString();
  private static final String PROGRAMME_NUMBER = UUID.randomUUID().toString();
  private static final String PROGRAMME_OWNER = UUID.randomUUID().toString();

  private static final UUID PROGRAMME_MEMBERSHIP_ID = UUID.randomUUID();
  private static final String PROGRAMME_MEMBERSHIP_TYPE = "SUBSTANTIVE";
  private static final LocalDate PROGRAMME_MEMBERSHIP_START_DATE = LocalDate.now().minusYears(2L);
  private static final LocalDate PROGRAMME_MEMBERSHIP_END_DATE = LocalDate.now().plusYears(2L);

  // Mock the event listeners, otherwise we can not control when enrichment happens.
  @MockBean
  private CurriculumEventListener curriculumEventListener;
  @MockBean
  private CurriculumMembershipEventListener curriculumMembershipEventListener;
  @MockBean
  private ProgrammeEventListener programmeEventListener;
  @MockBean
  private ProgrammeMembershipEventListener programmeMembershipEventListener;

  @Autowired
  private ProgrammeMembershipEnricherFacade enricher;

  @Autowired
  private CurriculumRepository curriculumRepository;
  @Autowired
  private CurriculumMembershipRepository curriculumMembershipRepository;
  @Autowired
  private PersonRepository personRepository;
  @Autowired
  private ProgrammeRepository programmeRepository;
  @Autowired
  private ProgrammeMembershipRepository programmeMembershipRepository;

  @MockBean
  private AmazonSNS amazonSns;
  @MockBean
  private AmazonSQSAsync amazonSqsAsync;

  @MockBean
  private QueueMessagingTemplate messagingTemplate;
  @MockBean
  private RestTemplate restTemplate;

  @Autowired
  private ObjectMapper objectMapper;

  private ProgrammeMembership programmeMembership;

  @BeforeEach
  void setUp() {
    Person person = new Person();
    person.setTisId(TRAINEE_ID);
    personRepository.save(person);

    Curriculum curriculum = new Curriculum();
    curriculum.setTisId(CURRICULUM_ID);
    curriculum.setData(Map.of(
        "id", CURRICULUM_ID,
        "name", CURRICULUM_NAME,
        "curriculumSubType", CURRICULUM_SUB_TYPE
    ));
    curriculumRepository.save(curriculum);

    CurriculumMembership curriculumMembership = new CurriculumMembership();
    curriculumMembership.setTisId(CURRICULUM_MEMBERSHIP_ID);
    curriculumMembership.setData(Map.of(
        "id", CURRICULUM_MEMBERSHIP_ID,
        "programmeMembershipUuid", PROGRAMME_MEMBERSHIP_ID.toString(),
        "curriculumId", CURRICULUM_ID,
        "curriculumStartDate", CURRICULUM_MEMBERSHIP_START_DATE.toString(),
        "curriculumEndDate", CURRICULUM_MEMBERSHIP_END_DATE.toString()
    ));
    curriculumMembershipRepository.save(curriculumMembership);

    Programme programme = new Programme();
    programme.setTisId(PROGRAMME_ID);
    programme.setData(Map.of(
        "id", PROGRAMME_ID,
        "programmeName", PROGRAMME_NAME,
        "programmeNumber", PROGRAMME_NUMBER,
        "owner", PROGRAMME_OWNER
    ));
    programmeRepository.save(programme);

    programmeMembership = new ProgrammeMembership();
    programmeMembership.setUuid(PROGRAMME_MEMBERSHIP_ID);
    programmeMembership.setPersonId(Long.parseLong(TRAINEE_ID));
    programmeMembership.setProgrammeId(Long.parseLong(PROGRAMME_ID));
    programmeMembership.setProgrammeMembershipType(PROGRAMME_MEMBERSHIP_TYPE);
    programmeMembership.setProgrammeStartDate(PROGRAMME_MEMBERSHIP_START_DATE);
    programmeMembership.setProgrammeEndDate(PROGRAMME_MEMBERSHIP_END_DATE);
    programmeMembershipRepository.save(programmeMembership);
  }

  @AfterEach
  void tearDown() {
    // Delete any additional data that was created by tests.
    curriculumRepository.deleteAll();
    curriculumMembershipRepository.deleteAll();
  }

  @Test
  void shouldRequestMissingCurriculumWhenTriggeredByProgrammeMembership()
      throws JsonProcessingException {
    curriculumRepository.deleteById(CURRICULUM_ID);

    enricher.enrich(programmeMembership);

    ArgumentCaptor<String> requestCaptor = ArgumentCaptor.forClass(String.class);
    verify(messagingTemplate).convertAndSend(any(String.class), requestCaptor.capture());

    DataRequest request = objectMapper.readValue(requestCaptor.getValue(), DataRequest.class);
    assertThat("Unexpected data request table.", request.table(), is(Curriculum.ENTITY_NAME));
    assertThat("Unexpected data request table.", request.id(), is(CURRICULUM_ID));
  }

  @Test
  void shouldRequestMissingCurriculumMembershipWhenTriggeredByProgrammeMembership()
      throws JsonProcessingException {
    curriculumMembershipRepository.deleteById(CURRICULUM_MEMBERSHIP_ID);

    enricher.enrich(programmeMembership);

    ArgumentCaptor<String> requestCaptor = ArgumentCaptor.forClass(String.class);
    verify(messagingTemplate).convertAndSend(any(String.class), requestCaptor.capture());

    record CurriculumMembershipDataRequest(String table, String programmeMembershipUuid) {

    }

    CurriculumMembershipDataRequest request = objectMapper.readValue(requestCaptor.getValue(),
        CurriculumMembershipDataRequest.class);
    assertThat("Unexpected data request table.", request.table(),
        is(CurriculumMembership.ENTITY_NAME));
    assertThat("Unexpected data request table.", request.programmeMembershipUuid(),
        is(PROGRAMME_MEMBERSHIP_ID.toString()));
  }

  @Test
  void shouldRequestMissingProgrammeWhenTriggeredByProgrammeMembership()
      throws JsonProcessingException {
    programmeRepository.deleteById(PROGRAMME_ID);

    enricher.enrich(programmeMembership);

    ArgumentCaptor<String> requestCaptor = ArgumentCaptor.forClass(String.class);
    verify(messagingTemplate).convertAndSend(any(String.class), requestCaptor.capture());

    DataRequest request = objectMapper.readValue(requestCaptor.getValue(), DataRequest.class);
    assertThat("Unexpected data request table.", request.table(), is(Programme.ENTITY_NAME));
    assertThat("Unexpected data request table.", request.id(), is(PROGRAMME_ID));
  }

  @Test
  void shouldEnrichProgrammeMembershipWithSingleCurriculumWhenTriggeredByProgrammeMembership() {
    enricher.enrich(programmeMembership);

    ArgumentCaptor<TraineeDetailsDto> traineeDetailsCaptor = ArgumentCaptor.forClass(
        TraineeDetailsDto.class);
    verify(restTemplate).patchForObject(any(), traineeDetailsCaptor.capture(), eq(Object.class),
        eq("programme-membership"), eq(TRAINEE_ID));

    TraineeDetailsDto traineeDetailsDto = traineeDetailsCaptor.getValue();
    assertThat("Unexpected TIS ID.", traineeDetailsDto.getTisId(),
        is(PROGRAMME_MEMBERSHIP_ID.toString()));
    assertThat("Unexpected trainee ID.", traineeDetailsDto.getTraineeTisId(), is(TRAINEE_ID));
    assertThat("Unexpected start date.", traineeDetailsDto.getStartDate(),
        is(PROGRAMME_MEMBERSHIP_START_DATE));
    assertThat("Unexpected end date.", traineeDetailsDto.getEndDate(),
        is(PROGRAMME_MEMBERSHIP_END_DATE));
    assertThat("Unexpected programme membership type.",
        traineeDetailsDto.getProgrammeMembershipType(), is(PROGRAMME_MEMBERSHIP_TYPE));
    assertThat("Unexpected programme name.", traineeDetailsDto.getProgrammeName(),
        is(PROGRAMME_NAME));
    assertThat("Unexpected programme number.", traineeDetailsDto.getProgrammeNumber(),
        is(PROGRAMME_NUMBER));
    assertThat("Unexpected programme TIS ID.", traineeDetailsDto.getProgrammeTisId(),
        is(PROGRAMME_ID));
    assertThat("Unexpected managing deanery.", traineeDetailsDto.getManagingDeanery(),
        is(PROGRAMME_OWNER));
    assertThat("Unexpected programme completion date.",
        traineeDetailsDto.getProgrammeCompletionDate(), is(CURRICULUM_MEMBERSHIP_END_DATE));

    Set<Map<String, String>> curricula = traineeDetailsDto.getCurricula();
    assertThat("Unexpected curricula count.", curricula.size(), is(1));

    Map<String, String> curriculum = curricula.iterator().next();
    assertThat("Unexpected curricula field count.", curriculum.size(), is(6));
    assertThat("Unexpected curriculum TIS ID.", curriculum.get("curriculumTisId"),
        is(CURRICULUM_ID));
    assertThat("Unexpected curriculum name.", curriculum.get("curriculumName"),
        is(CURRICULUM_NAME));
    assertThat("Unexpected curriculum sub-type.", curriculum.get("curriculumSubType"),
        is(CURRICULUM_SUB_TYPE));
    assertThat("Unexpected curriculum membership ID.", curriculum.get("curriculumMembershipId"),
        is(CURRICULUM_MEMBERSHIP_ID));
    assertThat("Unexpected curriculum start date.", curriculum.get("curriculumStartDate"),
        is(CURRICULUM_MEMBERSHIP_START_DATE.toString()));
    assertThat("Unexpected curriculum end date.", curriculum.get("curriculumEndDate"),
        is(CURRICULUM_MEMBERSHIP_END_DATE.toString()));
  }

  @Test
  void shouldEnrichProgrammeMembershipWithMultipleCurriculaWhenTriggeredByProgrammeMembership() {
    Curriculum curriculum = new Curriculum();
    String curriculumId = UUID.randomUUID().toString();
    curriculum.setTisId(curriculumId);
    curriculum.setData(Map.of(
        "id", curriculumId,
        "name", "Additional Curriculum",
        "curriculumSubType", CURRICULUM_SUB_TYPE
    ));
    curriculumRepository.save(curriculum);

    CurriculumMembership curriculumMembership = new CurriculumMembership();
    String curriculumMembershipId = UUID.randomUUID().toString();
    curriculumMembership.setTisId(curriculumMembershipId);
    LocalDate curriculumMembershipStartDate = CURRICULUM_MEMBERSHIP_START_DATE.plusMonths(1L);
    LocalDate curriculumMembershipEndDate = CURRICULUM_MEMBERSHIP_END_DATE.plusMonths(1L);
    curriculumMembership.setData(Map.of(
        "id", curriculumMembershipId,
        "programmeMembershipUuid", PROGRAMME_MEMBERSHIP_ID.toString(),
        "curriculumId", curriculumId,
        "curriculumStartDate", curriculumMembershipStartDate.toString(),
        "curriculumEndDate", curriculumMembershipEndDate.toString()
    ));
    curriculumMembershipRepository.save(curriculumMembership);

    enricher.enrich(programmeMembership);

    ArgumentCaptor<TraineeDetailsDto> traineeDetailsCaptor = ArgumentCaptor.forClass(
        TraineeDetailsDto.class);
    verify(restTemplate).patchForObject(any(), traineeDetailsCaptor.capture(), eq(Object.class),
        eq("programme-membership"), eq(TRAINEE_ID));

    TraineeDetailsDto traineeDetailsDto = traineeDetailsCaptor.getValue();
    assertThat("Unexpected TIS ID.", traineeDetailsDto.getTisId(),
        is(PROGRAMME_MEMBERSHIP_ID.toString()));
    assertThat("Unexpected trainee ID.", traineeDetailsDto.getTraineeTisId(), is(TRAINEE_ID));
    assertThat("Unexpected start date.", traineeDetailsDto.getStartDate(),
        is(PROGRAMME_MEMBERSHIP_START_DATE));
    assertThat("Unexpected end date.", traineeDetailsDto.getEndDate(),
        is(PROGRAMME_MEMBERSHIP_END_DATE));
    assertThat("Unexpected programme membership type.",
        traineeDetailsDto.getProgrammeMembershipType(), is(PROGRAMME_MEMBERSHIP_TYPE));
    assertThat("Unexpected programme name.", traineeDetailsDto.getProgrammeName(),
        is(PROGRAMME_NAME));
    assertThat("Unexpected programme number.", traineeDetailsDto.getProgrammeNumber(),
        is(PROGRAMME_NUMBER));
    assertThat("Unexpected programme TIS ID.", traineeDetailsDto.getProgrammeTisId(),
        is(PROGRAMME_ID));
    assertThat("Unexpected managing deanery.", traineeDetailsDto.getManagingDeanery(),
        is(PROGRAMME_OWNER));
    assertThat("Unexpected programme completion date.",
        traineeDetailsDto.getProgrammeCompletionDate(), is(curriculumMembershipEndDate));

    Set<Map<String, String>> curricula = traineeDetailsDto.getCurricula();
    assertThat("Unexpected curricula count.", curricula.size(), is(2));
    List<Map<String, String>> sortedCurricula = curricula.stream()
        .sorted(Comparator.comparing(c -> c.get("curriculumStartDate"))).toList();

    Map<String, String> curriculum1 = sortedCurricula.get(0);
    assertThat("Unexpected curricula field count.", curriculum1.size(), is(6));
    assertThat("Unexpected curriculum TIS ID.", curriculum1.get("curriculumTisId"),
        is(CURRICULUM_ID));
    assertThat("Unexpected curriculum name.", curriculum1.get("curriculumName"),
        is(CURRICULUM_NAME));
    assertThat("Unexpected curriculum sub-type.", curriculum1.get("curriculumSubType"),
        is(CURRICULUM_SUB_TYPE));
    assertThat("Unexpected curriculum membership ID.", curriculum1.get("curriculumMembershipId"),
        is(CURRICULUM_MEMBERSHIP_ID));
    assertThat("Unexpected curriculum start date.", curriculum1.get("curriculumStartDate"),
        is(CURRICULUM_MEMBERSHIP_START_DATE.toString()));
    assertThat("Unexpected curriculum end date.", curriculum1.get("curriculumEndDate"),
        is(CURRICULUM_MEMBERSHIP_END_DATE.toString()));

    Map<String, String> curriculum2 = sortedCurricula.get(1);
    assertThat("Unexpected curricula field count.", curriculum2.size(), is(6));
    assertThat("Unexpected curriculum TIS ID.", curriculum2.get("curriculumTisId"),
        is(curriculumId));
    assertThat("Unexpected curriculum name.", curriculum2.get("curriculumName"),
        is("Additional Curriculum"));
    assertThat("Unexpected curriculum sub-type.", curriculum2.get("curriculumSubType"),
        is(CURRICULUM_SUB_TYPE));
    assertThat("Unexpected curriculum membership ID.", curriculum2.get("curriculumMembershipId"),
        is(curriculumMembershipId));
    assertThat("Unexpected curriculum start date.", curriculum2.get("curriculumStartDate"),
        is(curriculumMembershipStartDate.toString()));
    assertThat("Unexpected curriculum end date.", curriculum2.get("curriculumEndDate"),
        is(curriculumMembershipEndDate.toString()));
  }

  private record DataRequest(String table, String id) {

  }
}
