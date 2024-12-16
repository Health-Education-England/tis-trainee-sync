/*
 * The MIT License (MIT)
 *
 * Copyright 2021 Crown Copyright (Health Education England)
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
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static uk.nhs.hee.tis.trainee.sync.event.DbcEventListener.DBC_ABBR;
import static uk.nhs.hee.tis.trainee.sync.event.DbcEventListener.DBC_DBC;
import static uk.nhs.hee.tis.trainee.sync.event.DbcEventListener.DBC_NAME;
import static uk.nhs.hee.tis.trainee.sync.event.HeeUserEventListener.HEE_USER_EMAIL;
import static uk.nhs.hee.tis.trainee.sync.event.HeeUserEventListener.HEE_USER_FIRST_NAME;
import static uk.nhs.hee.tis.trainee.sync.event.HeeUserEventListener.HEE_USER_GMC;
import static uk.nhs.hee.tis.trainee.sync.event.HeeUserEventListener.HEE_USER_LAST_NAME;
import static uk.nhs.hee.tis.trainee.sync.event.HeeUserEventListener.HEE_USER_NAME;
import static uk.nhs.hee.tis.trainee.sync.event.HeeUserEventListener.HEE_USER_PHONE;
import static uk.nhs.hee.tis.trainee.sync.event.LocalOfficeEventListener.LOCAL_OFFICE_ABBREVIATION;
import static uk.nhs.hee.tis.trainee.sync.event.LocalOfficeEventListener.LOCAL_OFFICE_NAME;
import static uk.nhs.hee.tis.trainee.sync.event.UserDesignatedBodyEventListener.USER_DB_DBC;
import static uk.nhs.hee.tis.trainee.sync.event.UserDesignatedBodyEventListener.USER_DB_USER_NAME;
import static uk.nhs.hee.tis.trainee.sync.event.UserRoleEventListener.USER_ROLE_ROLE_NAME;
import static uk.nhs.hee.tis.trainee.sync.event.UserRoleEventListener.USER_ROLE_USER_NAME;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.lang.reflect.Field;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.util.ReflectionUtils;
import uk.nhs.hee.tis.trainee.sync.dto.ProgrammeMembershipEventDto;
import uk.nhs.hee.tis.trainee.sync.mapper.AggregateMapper;
import uk.nhs.hee.tis.trainee.sync.mapper.AggregateMapperImpl;
import uk.nhs.hee.tis.trainee.sync.mapper.HeeUserMapperImpl;
import uk.nhs.hee.tis.trainee.sync.mapper.ProgrammeMembershipEventMapper;
import uk.nhs.hee.tis.trainee.sync.mapper.ProgrammeMembershipEventMapperImpl;
import uk.nhs.hee.tis.trainee.sync.mapper.ProgrammeMembershipMapper;
import uk.nhs.hee.tis.trainee.sync.mapper.ProgrammeMembershipMapperImpl;
import uk.nhs.hee.tis.trainee.sync.model.ConditionsOfJoining;
import uk.nhs.hee.tis.trainee.sync.model.Curriculum;
import uk.nhs.hee.tis.trainee.sync.model.CurriculumMembership;
import uk.nhs.hee.tis.trainee.sync.model.Dbc;
import uk.nhs.hee.tis.trainee.sync.model.HeeUser;
import uk.nhs.hee.tis.trainee.sync.model.LocalOffice;
import uk.nhs.hee.tis.trainee.sync.model.Programme;
import uk.nhs.hee.tis.trainee.sync.model.ProgrammeMembership;
import uk.nhs.hee.tis.trainee.sync.model.Record;
import uk.nhs.hee.tis.trainee.sync.model.Specialty;
import uk.nhs.hee.tis.trainee.sync.model.UserDesignatedBody;
import uk.nhs.hee.tis.trainee.sync.model.UserRole;
import uk.nhs.hee.tis.trainee.sync.service.ConditionsOfJoiningSyncService;
import uk.nhs.hee.tis.trainee.sync.service.CurriculumMembershipSyncService;
import uk.nhs.hee.tis.trainee.sync.service.CurriculumSyncService;
import uk.nhs.hee.tis.trainee.sync.service.DbcSyncService;
import uk.nhs.hee.tis.trainee.sync.service.HeeUserSyncService;
import uk.nhs.hee.tis.trainee.sync.service.LocalOfficeSyncService;
import uk.nhs.hee.tis.trainee.sync.service.ProgrammeMembershipSyncService;
import uk.nhs.hee.tis.trainee.sync.service.ProgrammeSyncService;
import uk.nhs.hee.tis.trainee.sync.service.SpecialtySyncService;
import uk.nhs.hee.tis.trainee.sync.service.TcsSyncService;
import uk.nhs.hee.tis.trainee.sync.service.UserDesignatedBodySyncService;
import uk.nhs.hee.tis.trainee.sync.service.UserRoleSyncService;

@ExtendWith(MockitoExtension.class)
class ProgrammeMembershipEnricherFacadeTest {

  private static final String PROGRAMME_1_ID = "1";
  private static final String PROGRAMME_1_NAME = "programme One";
  private static final String PROGRAMME_1_NUMBER = "programme No. One";
  private static final String PROGRAMME_1_OWNER = "programme One owner";
  private static final String CURRICULUM_1_ID = "1";
  private static final String CURRICULUM_1_NAME = "curriculum One";
  private static final LocalDate CURRICULUM_1_START_DATE = LocalDate.parse("2020-01-01");
  private static final LocalDate CURRICULUM_1_END_DATE = LocalDate.parse("2021-01-01");
  private static final String CURRICULUM_2_ID = "2";

  private static final String CURRICULUM_MEMBERSHIP_1_ID = UUID.randomUUID().toString();
  private static final String CURRICULUM_MEMBERSHIP_2_ID = UUID.randomUUID().toString();

  private static final String ALL_TIS_ID = UUID.randomUUID().toString();
  private static final String ALL_PERSON_ID = "1";
  private static final String ALL_PROGRAMME_MEMBERSHIP_TYPE = "SUBSTANTIVE";
  private static final LocalDate ALL_PROGRAMME_START_DATE = LocalDate.parse("2020-01-01");
  private static final LocalDate ALL_PROGRAMME_END_DATE = LocalDate.parse("2021-01-01");

  //fields in curriculum/programme sync repo documents
  private static final String CURRICULUM_NAME = "name";
  private static final String CURRICULUM_SPECIALTY_ID = "specialtyId";
  private static final String PROGRAMME_NAME = "programmeName";
  private static final String PROGRAMME_NUMBER = "programmeNumber";
  private static final String PROGRAMME_OWNER = "owner";

  private static final String DATA_CURRICULUM_ID = "curriculumId";
  private static final String DATA_CURRICULUM_START_DATE = "curriculumStartDate";
  private static final String DATA_CURRICULUM_END_DATE = "curriculumEndDate";

  private static final String SPECIALTY_NAME = "name";
  private static final String SPECIALTY_CODE = "specialtyCode";
  private static final String SPECIALTY_BLOCK_INDEMNITY = "blockIndemnity";
  private static final String SPECIALTY_1_ID = "154";
  private static final String SPECIALTY_1_NAME = "Medical Microbiology";
  private static final String SPECIALTY_1_CODE = "ABC123";
  private static final String SPECIALTY_1_BLOCK_INDEMNITY = "true";

  // processed fields in programmeMembership DTO passed to trainee-details for persisting
  private static final String PROGRAMME_MEMBERSHIP_DATA_PROGRAMME_NAME = "programmeName";
  private static final String PROGRAMME_MEMBERSHIP_DATA_PROGRAMME_NUMBER = "programmeNumber";
  private static final String PROGRAMME_MEMBERSHIP_DATA_MANAGING_DEANERY = "managingDeanery";
  private static final String PROGRAMME_MEMBERSHIP_DATA_DESIGNATED_BODY = "designatedBody";
  private static final String PROGRAMME_MEMBERSHIP_DATA_CONDITIONS_OF_JOINING
      = "conditionsOfJoining";
  private static final String PROGRAMME_MEMBERSHIP_DATA_RESPONSIBLE_OFFICER = "responsibleOfficer";
  private static final String PROGRAMME_MEMBERSHIP_DATA_COJ_SIGNED_AT = "signedAt";
  private static final String PROGRAMME_MEMBERSHIP_DATA_COJ_VERSION = "version";
  private static final String PROGRAMME_MEMBERSHIP_DATA_CURRICULA = "curricula";
  private static final String PROGRAMME_MEMBERSHIP_DATA_CURRICULUM_NAME = "curriculumName";
  private static final String PROGRAMME_MEMBERSHIP_DATA_CURRICULUM_SPECIALTY
      = "curriculumSpecialty";
  private static final String PROGRAMME_MEMBERSHIP_DATA_CURRICULUM_SPECIALTY_CODE
      = "curriculumSpecialtyCode";
  private static final String PROGRAMME_MEMBERSHIP_DATA_CURRICULUM_SPECIALTY_BLOCK_INDEMNITY
      = "curriculumSpecialtyBlockIndemnity";
  private static final String PROGRAMME_MEMBERSHIP_DATA_CURRICULUM_START_DATE
      = "curriculumStartDate";
  private static final String PROGRAMME_MEMBERSHIP_DATA_CURRICULUM_END_DATE
      = "curriculumEndDate";

  private static final String LOCAL_OFFICE_ABBREVIATION_VALUE = "HEEOE";
  private static final String DBC_NAME_VALUE = "the dbc";
  private static final String DBC_DBC_VALUE = "1-DBC01";
  private static final String ROLE_VALUE = "RVOfficer";
  private static final String USER_NAME_VALUE = "user@test";
  private static final String USER_FIRST_NAME_VALUE = "user firstname";
  private static final String USER_LAST_NAME_VALUE = "user lastname";
  private static final String USER_EMAIL_VALUE = "user@test.com";
  private static final String USER_GMC_VALUE = "123abc";
  private static final String USER_PHONE_VALUE = "1234";

  private static final Instant COJ_SIGNED_AT = Instant.now();
  private static final String COJ_VERSION = "GG9";

  @InjectMocks
  @Spy
  private ProgrammeMembershipEnricherFacade enricher;

  @Mock
  private ConditionsOfJoiningSyncService conditionsOfJoiningService;

  @Mock
  private CurriculumMembershipSyncService curriculumMembershipService;

  @Mock
  private ProgrammeMembershipSyncService programmeMembershipService;

  @Mock
  private ProgrammeSyncService programmeService;

  @Mock
  private CurriculumSyncService curriculumService;

  @Mock
  private SpecialtySyncService specialtyService;

  @Mock
  private LocalOfficeSyncService localOfficeService;

  @Mock
  private DbcSyncService dbcService;

  @Mock
  private UserDesignatedBodySyncService udbService;

  @Mock
  private UserRoleSyncService userRoleService;

  @Mock
  private HeeUserSyncService heeUserService;

  @Mock
  private TcsSyncService tcsSyncService;

  @Spy
  private AggregateMapper aggregateMapper = new AggregateMapperImpl();

  @Spy
  private ProgrammeMembershipMapper programmeMembershipMapper = new ProgrammeMembershipMapperImpl();

  @Spy
  private ProgrammeMembershipEventMapper programmeMembershipEventMapper
      = new ProgrammeMembershipEventMapperImpl();

  @BeforeEach
  void setUp() {
    Field field = ReflectionUtils.findRequiredField(AggregateMapperImpl.class, "heeUserMapper");
    field.setAccessible(true);
    ReflectionUtils.setField(field, aggregateMapper, new HeeUserMapperImpl());
  }

  @Test
  void shouldEnrichProgrammeMembershipWhenProgrammeAndCurriculumAndSpecialtyExist()
      throws JsonProcessingException {
    ProgrammeMembership programmeMembership
        = buildEnrichableProgrammeMembershipWithAllMocksEnabled();

    enricher.enrich(programmeMembership);

    verify(programmeMembershipService, never()).request(any());
    verify(programmeService, never()).request(anyString());
    verify(curriculumService, never()).request(anyString());
    verify(specialtyService, never()).request(anyString());
    verify(localOfficeService, never()).requestByName(anyString());
    verify(dbcService, never()).requestByAbbr(anyString());

    ArgumentCaptor<Record> recordCaptor = ArgumentCaptor.forClass(Record.class);
    verify(tcsSyncService).syncRecord(recordCaptor.capture());

    Map<String, String> programmeMembershipData = recordCaptor.getValue().getData();
    assertThat("Unexpected programme name.",
        programmeMembershipData.get(PROGRAMME_MEMBERSHIP_DATA_PROGRAMME_NAME),
        is(PROGRAMME_1_NAME));
    assertThat("Unexpected programme number.",
        programmeMembershipData.get(PROGRAMME_MEMBERSHIP_DATA_PROGRAMME_NUMBER),
        is(PROGRAMME_1_NUMBER));
    assertThat("Unexpected managing deanery.",
        programmeMembershipData.get(PROGRAMME_MEMBERSHIP_DATA_MANAGING_DEANERY),
        is(PROGRAMME_1_OWNER));
    assertThat("Unexpected designated body.",
        programmeMembershipData.get(PROGRAMME_MEMBERSHIP_DATA_DESIGNATED_BODY),
        is(DBC_NAME_VALUE));

    Set<Map<String, String>> curricula = new ObjectMapper().readValue(
        programmeMembershipData.get(PROGRAMME_MEMBERSHIP_DATA_CURRICULA), new TypeReference<>() {
        });
    assertThat("Unexpected curricula size.", curricula.size(), is(1));

    Map<String, String> curriculumData = curricula.iterator().next();
    assertThat("Unexpected curriculum name.",
        curriculumData.get(PROGRAMME_MEMBERSHIP_DATA_CURRICULUM_NAME), is(CURRICULUM_1_NAME));
    assertThat("Unexpected curriculum specialty.",
        curriculumData.get(PROGRAMME_MEMBERSHIP_DATA_CURRICULUM_SPECIALTY),
        is(SPECIALTY_1_NAME));
    assertThat("Unexpected curriculum specialty code.",
        curriculumData.get(PROGRAMME_MEMBERSHIP_DATA_CURRICULUM_SPECIALTY_CODE),
        is(SPECIALTY_1_CODE));
    assertThat("Unexpected curriculum specialty block indemnity.",
        curriculumData.get(PROGRAMME_MEMBERSHIP_DATA_CURRICULUM_SPECIALTY_BLOCK_INDEMNITY),
        is(SPECIALTY_1_BLOCK_INDEMNITY));
    assertThat("Unexpected curriculum start date.",
        curriculumData.get(PROGRAMME_MEMBERSHIP_DATA_CURRICULUM_START_DATE),
        is(CURRICULUM_1_START_DATE.toString()));
    assertThat("Unexpected curriculum end date.",
        curriculumData.get(PROGRAMME_MEMBERSHIP_DATA_CURRICULUM_END_DATE),
        is(CURRICULUM_1_END_DATE.toString()));

    Map<String, String> conditionsOfJoiningData = new ObjectMapper().readValue(
        programmeMembershipData.get(PROGRAMME_MEMBERSHIP_DATA_CONDITIONS_OF_JOINING),
        new TypeReference<>() {
        });
    assertThat("Unexpected Conditions of joining signed at.",
        conditionsOfJoiningData.get(PROGRAMME_MEMBERSHIP_DATA_COJ_SIGNED_AT),
        is(COJ_SIGNED_AT.toString()));
    assertThat("Unexpected Conditions of joining version.",
        conditionsOfJoiningData.get(PROGRAMME_MEMBERSHIP_DATA_COJ_VERSION),
        is(COJ_VERSION));

    Map<String, String> responsibleOfficerData = new ObjectMapper().readValue(
        programmeMembershipData.get(PROGRAMME_MEMBERSHIP_DATA_RESPONSIBLE_OFFICER),
        new TypeReference<>() {
        });
    assertThat("Unexpected responsible officer last name.",
        responsibleOfficerData.get(HEE_USER_LAST_NAME), is(USER_LAST_NAME_VALUE));
    assertThat("Unexpected responsible officer first name.",
        responsibleOfficerData.get(HEE_USER_FIRST_NAME), is(USER_FIRST_NAME_VALUE));
    assertThat("Unexpected responsible officer email.",
        responsibleOfficerData.get(HEE_USER_EMAIL), is(USER_EMAIL_VALUE));
    assertThat("Unexpected responsible officer gmc.",
        responsibleOfficerData.get(HEE_USER_GMC), is(USER_GMC_VALUE));
    assertThat("Unexpected responsible officer phone.",
        responsibleOfficerData.get(HEE_USER_PHONE), is(USER_PHONE_VALUE));
  }

  @Test
  void shouldEnrichProgrammeMembershipWhenResponsibleOfficerHeeUserNotExist()
      throws JsonProcessingException {
    final ProgrammeMembership programmeMembership
        = buildEnrichableProgrammeMembershipWithAllMocksEnabled();

    //override enrichable programme membership
    Mockito.reset(heeUserService);
    when(heeUserService.findByName(anyString())).thenReturn(Optional.empty());

    enricher.enrich(programmeMembership);

    ArgumentCaptor<Record> recordCaptor = ArgumentCaptor.forClass(Record.class);
    verify(tcsSyncService).syncRecord(recordCaptor.capture());

    Map<String, String> programmeMembershipData = recordCaptor.getValue().getData();
    Map<String, String> responsibleOfficerData = new ObjectMapper().readValue(
        programmeMembershipData.get(PROGRAMME_MEMBERSHIP_DATA_RESPONSIBLE_OFFICER),
        new TypeReference<>() {
        });
    assertThat("Unexpected responsible officer.", responsibleOfficerData, is(nullValue()));
  }

  @Test
  void shouldEnrichProgrammeMembershipWhenResponsibleOfficerNotExist()
      throws JsonProcessingException {
    final ProgrammeMembership programmeMembership
        = buildEnrichableProgrammeMembershipWithAllMocksEnabled();

    //override enrichable programme membership
    Mockito.reset(userRoleService);
    Mockito.reset(heeUserService);
    when(userRoleService.findRvOfficerRoleByUserName(anyString())).thenReturn(Optional.empty());

    enricher.enrich(programmeMembership);

    ArgumentCaptor<Record> recordCaptor = ArgumentCaptor.forClass(Record.class);
    verify(tcsSyncService).syncRecord(recordCaptor.capture());

    Map<String, String> programmeMembershipData = recordCaptor.getValue().getData();
    Map<String, String> responsibleOfficerData = new ObjectMapper().readValue(
        programmeMembershipData.get(PROGRAMME_MEMBERSHIP_DATA_RESPONSIBLE_OFFICER),
        new TypeReference<>() {
        });
    assertThat("Unexpected responsible officer.", responsibleOfficerData, is(nullValue()));
  }

  @Test
  void shouldEnrichProgrammeMembershipWhenUserDesignatedBodyNotExist()
      throws JsonProcessingException {
    final ProgrammeMembership programmeMembership
        = buildEnrichableProgrammeMembershipWithAllMocksEnabled();

    //override enrichable programme membership
    Mockito.reset(udbService);
    Mockito.reset(userRoleService);
    Mockito.reset(heeUserService);
    when(udbService.findByDbc(anyString())).thenReturn(Set.of());

    enricher.enrich(programmeMembership);

    ArgumentCaptor<Record> recordCaptor = ArgumentCaptor.forClass(Record.class);
    verify(tcsSyncService).syncRecord(recordCaptor.capture());

    Map<String, String> programmeMembershipData = recordCaptor.getValue().getData();
    Map<String, String> responsibleOfficerData = new ObjectMapper().readValue(
        programmeMembershipData.get(PROGRAMME_MEMBERSHIP_DATA_RESPONSIBLE_OFFICER),
        new TypeReference<>() {
        });
    assertThat("Unexpected responsible officer.", responsibleOfficerData, is(nullValue()));
  }

  @Test
  void shouldEnrichProgrammeMembershipWithFirstResponsibleOfficerIfMultiple()
      throws JsonProcessingException {
    final ProgrammeMembership programmeMembership
        = buildEnrichableProgrammeMembershipWithAllMocksEnabled();

    //override enrichable programme membership
    Mockito.reset(userRoleService);
    UserRole userRole = new UserRole();
    userRole.setData(Map.of(
        USER_ROLE_USER_NAME, USER_NAME_VALUE,
        USER_ROLE_ROLE_NAME, ROLE_VALUE
    ));
    UserRole userRole2 = new UserRole();
    userRole.setData(Map.of(
        USER_ROLE_USER_NAME, "some other user",
        USER_ROLE_ROLE_NAME, ROLE_VALUE
    ));
    when(userRoleService.findRvOfficerRoleByUserName(anyString()))
        .thenReturn(Optional.of(userRole), Optional.of(userRole2));

    enricher.enrich(programmeMembership);

    ArgumentCaptor<Record> recordCaptor = ArgumentCaptor.forClass(Record.class);
    verify(tcsSyncService).syncRecord(recordCaptor.capture());

    verify(userRoleService).findRvOfficerRoleByUserName(anyString()); //only called once

    Map<String, String> programmeMembershipData = recordCaptor.getValue().getData();
    Map<String, String> responsibleOfficerData = new ObjectMapper().readValue(
        programmeMembershipData.get(PROGRAMME_MEMBERSHIP_DATA_RESPONSIBLE_OFFICER),
        new TypeReference<>() {
        });
    assertThat("Unexpected responsible officer last name.",
        responsibleOfficerData.get(HEE_USER_LAST_NAME), is(USER_LAST_NAME_VALUE));
    assertThat("Unexpected responsible officer first name.",
        responsibleOfficerData.get(HEE_USER_FIRST_NAME), is(USER_FIRST_NAME_VALUE));
    assertThat("Unexpected responsible officer email.",
        responsibleOfficerData.get(HEE_USER_EMAIL), is(USER_EMAIL_VALUE));
    assertThat("Unexpected responsible officer gmc.",
        responsibleOfficerData.get(HEE_USER_GMC), is(USER_GMC_VALUE));
    assertThat("Unexpected responsible officer phone.",
        responsibleOfficerData.get(HEE_USER_PHONE), is(USER_PHONE_VALUE));
  }

  @Test
  void shouldNotEnrichProgrammeMembershipWhenCurriculumMembershipNotExist() {
    ProgrammeMembership programmeMembership = new ProgrammeMembership();
    programmeMembership.setUuid(UUID.fromString(ALL_TIS_ID));
    programmeMembership.setProgrammeId(Long.parseLong(PROGRAMME_1_ID));

    when(curriculumMembershipService.findByProgrammeMembershipUuid(ALL_TIS_ID)).thenReturn(
        Set.of());

    Programme programme = new Programme();
    programme.setTisId(PROGRAMME_1_ID);
    when(programmeService.findById(PROGRAMME_1_ID)).thenReturn(Optional.of(programme));

    enricher.enrich(programmeMembership);

    verify(curriculumMembershipService).requestForProgrammeMembership(ALL_TIS_ID);
    verifyNoInteractions(tcsSyncService);
  }

  @Test
  void shouldNotEnrichProgrammeMembershipWhenCurriculumNotExist() {
    ProgrammeMembership programmeMembership = new ProgrammeMembership();
    programmeMembership.setUuid(UUID.fromString(ALL_TIS_ID));
    programmeMembership.setProgrammeId(Long.parseLong(PROGRAMME_1_ID));

    CurriculumMembership curriculumMembership1 = new CurriculumMembership();
    curriculumMembership1.setTisId(CURRICULUM_MEMBERSHIP_1_ID);
    curriculumMembership1.setData(Map.of(DATA_CURRICULUM_ID, CURRICULUM_1_ID));
    CurriculumMembership curriculumMembership2 = new CurriculumMembership();
    curriculumMembership2.setTisId(CURRICULUM_MEMBERSHIP_2_ID);
    curriculumMembership2.setData(Map.of(DATA_CURRICULUM_ID, CURRICULUM_2_ID));
    when(curriculumMembershipService.findByProgrammeMembershipUuid(ALL_TIS_ID)).thenReturn(
        Set.of(curriculumMembership1, curriculumMembership2));

    Curriculum curriculum1 = new Curriculum();
    curriculum1.setTisId(CURRICULUM_1_ID);
    curriculum1.setData(Map.of(
        CURRICULUM_SPECIALTY_ID, SPECIALTY_1_ID
    ));
    when(curriculumService.findById(CURRICULUM_1_ID)).thenReturn(Optional.of(curriculum1));
    when(curriculumService.findById(CURRICULUM_2_ID)).thenReturn(Optional.empty());

    Specialty specialty = new Specialty();
    specialty.setData(Map.of(
        SPECIALTY_NAME, SPECIALTY_1_NAME
    ));
    when(specialtyService.findById(SPECIALTY_1_ID)).thenReturn(Optional.of(specialty));

    Programme programme = new Programme();
    programme.setTisId(PROGRAMME_1_ID);
    when(programmeService.findById(PROGRAMME_1_ID)).thenReturn(Optional.of(programme));

    enricher.enrich(programmeMembership);

    verify(curriculumService, never()).request(CURRICULUM_1_ID);
    verify(curriculumService).request(CURRICULUM_2_ID);
    verifyNoInteractions(tcsSyncService);
  }

  @Test
  void shouldNotEnrichProgrammeMembershipWhenSpecialtyNotExist() {
    ProgrammeMembership programmeMembership = new ProgrammeMembership();
    programmeMembership.setUuid(UUID.fromString(ALL_TIS_ID));
    programmeMembership.setProgrammeId(Long.parseLong(PROGRAMME_1_ID));

    CurriculumMembership curriculumMembership = new CurriculumMembership();
    curriculumMembership.setTisId(CURRICULUM_MEMBERSHIP_1_ID);
    curriculumMembership.setData(Map.of(DATA_CURRICULUM_ID, CURRICULUM_1_ID));
    when(curriculumMembershipService.findByProgrammeMembershipUuid(ALL_TIS_ID)).thenReturn(
        Set.of(curriculumMembership));

    Curriculum curriculum1 = new Curriculum();
    curriculum1.setTisId(CURRICULUM_1_ID);
    curriculum1.setData(Map.of(
        CURRICULUM_SPECIALTY_ID, SPECIALTY_1_ID
    ));
    when(curriculumService.findById(CURRICULUM_1_ID)).thenReturn(Optional.of(curriculum1));

    when(specialtyService.findById(SPECIALTY_1_ID)).thenReturn(Optional.empty());

    Programme programme = new Programme();
    programme.setTisId(PROGRAMME_1_ID);
    when(programmeService.findById(PROGRAMME_1_ID)).thenReturn(Optional.of(programme));

    enricher.enrich(programmeMembership);

    verify(specialtyService).request(SPECIALTY_1_ID);
    verifyNoInteractions(tcsSyncService);
  }

  @Test
  void shouldNotEnrichProgrammeMembershipWhenProgrammeIdNull() {
    ProgrammeMembership programmeMembership = new ProgrammeMembership();
    programmeMembership.setUuid(UUID.fromString(ALL_TIS_ID));
    programmeMembership.setProgrammeId(null);

    CurriculumMembership curriculumMembership = new CurriculumMembership();
    curriculumMembership.setTisId(CURRICULUM_MEMBERSHIP_1_ID);
    curriculumMembership.setData(Map.of(DATA_CURRICULUM_ID, CURRICULUM_1_ID));
    when(curriculumMembershipService.findByProgrammeMembershipUuid(ALL_TIS_ID)).thenReturn(
        Set.of(curriculumMembership));

    Curriculum curriculum = new Curriculum();
    curriculum.setTisId(CURRICULUM_1_ID);
    when(curriculumService.findById(CURRICULUM_1_ID)).thenReturn(Optional.of(curriculum));

    enricher.enrich(programmeMembership);

    verifyNoInteractions(programmeService);
    verifyNoInteractions(tcsSyncService);
  }

  @Test
  void shouldNotEnrichProgrammeMembershipWhenProgrammeNotExist() {
    ProgrammeMembership programmeMembership = new ProgrammeMembership();
    programmeMembership.setUuid(UUID.fromString(ALL_TIS_ID));
    programmeMembership.setProgrammeId(Long.parseLong(PROGRAMME_1_ID));

    CurriculumMembership curriculumMembership = new CurriculumMembership();
    curriculumMembership.setTisId(CURRICULUM_MEMBERSHIP_1_ID);
    curriculumMembership.setData(Map.of(DATA_CURRICULUM_ID, CURRICULUM_1_ID));
    when(curriculumMembershipService.findByProgrammeMembershipUuid(ALL_TIS_ID)).thenReturn(
        Set.of(curriculumMembership));

    Curriculum curriculum = new Curriculum();
    curriculum.setTisId(CURRICULUM_1_ID);
    when(curriculumService.findById(CURRICULUM_1_ID)).thenReturn(Optional.of(curriculum));

    when(programmeService.findById(PROGRAMME_1_ID)).thenReturn(Optional.empty());

    enricher.enrich(programmeMembership);

    verify(programmeService).request(PROGRAMME_1_ID);
    verifyNoInteractions(tcsSyncService);
  }

  @Test
  void shouldEnrichProgrammeMembershipWhenLocalOfficeNotExist() {
    ProgrammeMembership programmeMembership = new ProgrammeMembership();
    programmeMembership.setUuid(UUID.fromString(ALL_TIS_ID));
    programmeMembership.setPersonId(Long.parseLong(ALL_PERSON_ID));
    programmeMembership.setProgrammeId(Long.parseLong(PROGRAMME_1_ID));
    programmeMembership.setProgrammeMembershipType(ALL_PROGRAMME_MEMBERSHIP_TYPE);
    programmeMembership.setProgrammeStartDate(ALL_PROGRAMME_START_DATE);
    programmeMembership.setProgrammeEndDate(ALL_PROGRAMME_END_DATE);

    CurriculumMembership curriculumMembership = new CurriculumMembership();
    curriculumMembership.setTisId(CURRICULUM_MEMBERSHIP_1_ID);
    curriculumMembership.setData(Map.of(DATA_CURRICULUM_ID, CURRICULUM_1_ID,
        DATA_CURRICULUM_START_DATE, CURRICULUM_1_START_DATE.toString(),
        DATA_CURRICULUM_END_DATE, CURRICULUM_1_END_DATE.toString()));
    when(curriculumMembershipService.findByProgrammeMembershipUuid(ALL_TIS_ID)).thenReturn(
        Set.of(curriculumMembership));

    Curriculum curriculum = new Curriculum();
    curriculum.setTisId(CURRICULUM_1_ID);
    curriculum.setData(Map.of(
        CURRICULUM_NAME, CURRICULUM_1_NAME,
        CURRICULUM_SPECIALTY_ID, SPECIALTY_1_ID
    ));
    when(curriculumService.findById(CURRICULUM_1_ID)).thenReturn(Optional.of(curriculum));

    Specialty specialty = new Specialty();
    specialty.setData(Map.of(
        SPECIALTY_NAME, SPECIALTY_1_NAME,
        SPECIALTY_CODE, SPECIALTY_1_CODE,
        SPECIALTY_BLOCK_INDEMNITY, SPECIALTY_1_BLOCK_INDEMNITY
    ));
    when(specialtyService.findById(SPECIALTY_1_ID)).thenReturn(Optional.of(specialty));

    ConditionsOfJoining conditionsOfJoining = new ConditionsOfJoining();
    conditionsOfJoining.setProgrammeMembershipUuid(ALL_TIS_ID);
    conditionsOfJoining.setSignedAt(COJ_SIGNED_AT);
    conditionsOfJoining.setVersion(COJ_VERSION);
    when(conditionsOfJoiningService.findById(ALL_TIS_ID))
        .thenReturn(Optional.of(conditionsOfJoining));

    Programme programme = new Programme();
    programme.setTisId(PROGRAMME_1_ID);
    programme.setData(Map.of(
        PROGRAMME_NAME, PROGRAMME_1_NAME,
        PROGRAMME_NUMBER, PROGRAMME_1_NUMBER,
        PROGRAMME_OWNER, PROGRAMME_1_OWNER
    ));
    when(programmeService.findById(PROGRAMME_1_ID)).thenReturn(Optional.of(programme));

    when(localOfficeService.findByName(PROGRAMME_1_OWNER)).thenReturn(Optional.empty());

    enricher.enrich(programmeMembership);

    verify(localOfficeService).requestByName(PROGRAMME_1_OWNER);

    ArgumentCaptor<Record> recordCaptor = ArgumentCaptor.forClass(Record.class);
    verify(tcsSyncService).syncRecord(recordCaptor.capture());

    Map<String, String> programmeMembershipData = recordCaptor.getValue().getData();
    assertThat("Unexpected designated body.",
        programmeMembershipData.get(PROGRAMME_MEMBERSHIP_DATA_DESIGNATED_BODY),
        is(nullValue()));
  }

  @Test
  void shouldEnrichProgrammeMembershipWhenDbcNotExist() {
    ProgrammeMembership programmeMembership = new ProgrammeMembership();
    programmeMembership.setUuid(UUID.fromString(ALL_TIS_ID));
    programmeMembership.setPersonId(Long.parseLong(ALL_PERSON_ID));
    programmeMembership.setProgrammeId(Long.parseLong(PROGRAMME_1_ID));
    programmeMembership.setProgrammeMembershipType(ALL_PROGRAMME_MEMBERSHIP_TYPE);
    programmeMembership.setProgrammeStartDate(ALL_PROGRAMME_START_DATE);
    programmeMembership.setProgrammeEndDate(ALL_PROGRAMME_END_DATE);

    CurriculumMembership curriculumMembership = new CurriculumMembership();
    curriculumMembership.setTisId(CURRICULUM_MEMBERSHIP_1_ID);
    curriculumMembership.setData(Map.of(DATA_CURRICULUM_ID, CURRICULUM_1_ID,
        DATA_CURRICULUM_START_DATE, CURRICULUM_1_START_DATE.toString(),
        DATA_CURRICULUM_END_DATE, CURRICULUM_1_END_DATE.toString()));
    when(curriculumMembershipService.findByProgrammeMembershipUuid(ALL_TIS_ID)).thenReturn(
        Set.of(curriculumMembership));

    Curriculum curriculum = new Curriculum();
    curriculum.setTisId(CURRICULUM_1_ID);
    curriculum.setData(Map.of(
        CURRICULUM_NAME, CURRICULUM_1_NAME,
        CURRICULUM_SPECIALTY_ID, SPECIALTY_1_ID
    ));
    when(curriculumService.findById(CURRICULUM_1_ID)).thenReturn(Optional.of(curriculum));

    Specialty specialty = new Specialty();
    specialty.setData(Map.of(
        SPECIALTY_NAME, SPECIALTY_1_NAME,
        SPECIALTY_CODE, SPECIALTY_1_CODE,
        SPECIALTY_BLOCK_INDEMNITY, SPECIALTY_1_BLOCK_INDEMNITY
    ));
    when(specialtyService.findById(SPECIALTY_1_ID)).thenReturn(Optional.of(specialty));

    ConditionsOfJoining conditionsOfJoining = new ConditionsOfJoining();
    conditionsOfJoining.setProgrammeMembershipUuid(ALL_TIS_ID);
    conditionsOfJoining.setSignedAt(COJ_SIGNED_AT);
    conditionsOfJoining.setVersion(COJ_VERSION);
    when(conditionsOfJoiningService.findById(ALL_TIS_ID))
        .thenReturn(Optional.of(conditionsOfJoining));

    Programme programme = new Programme();
    programme.setTisId(PROGRAMME_1_ID);
    programme.setData(Map.of(
        PROGRAMME_NAME, PROGRAMME_1_NAME,
        PROGRAMME_NUMBER, PROGRAMME_1_NUMBER,
        PROGRAMME_OWNER, PROGRAMME_1_OWNER
    ));
    when(programmeService.findById(PROGRAMME_1_ID)).thenReturn(Optional.of(programme));

    LocalOffice localOffice = new LocalOffice();
    localOffice.setData(Map.of(
        LOCAL_OFFICE_NAME, PROGRAMME_1_OWNER,
        LOCAL_OFFICE_ABBREVIATION, LOCAL_OFFICE_ABBREVIATION_VALUE
    ));
    when(localOfficeService.findByName(PROGRAMME_1_OWNER)).thenReturn(Optional.of(localOffice));

    when(dbcService.findByAbbr(LOCAL_OFFICE_ABBREVIATION_VALUE)).thenReturn(Optional.empty());

    enricher.enrich(programmeMembership);

    verify(localOfficeService, never()).requestByName(anyString());
    verify(dbcService).requestByAbbr(LOCAL_OFFICE_ABBREVIATION_VALUE);

    ArgumentCaptor<Record> recordCaptor = ArgumentCaptor.forClass(Record.class);
    verify(tcsSyncService).syncRecord(recordCaptor.capture());

    Map<String, String> programmeMembershipData = recordCaptor.getValue().getData();
    assertThat("Unexpected designated body.",
        programmeMembershipData.get(PROGRAMME_MEMBERSHIP_DATA_DESIGNATED_BODY),
        is(nullValue()));
  }

  @Test
  void shouldEnrichProgrammeMembershipWhenLocalOfficeAbbreviationMissing() {
    ProgrammeMembership programmeMembership = new ProgrammeMembership();
    programmeMembership.setUuid(UUID.fromString(ALL_TIS_ID));
    programmeMembership.setPersonId(Long.parseLong(ALL_PERSON_ID));
    programmeMembership.setProgrammeId(Long.parseLong(PROGRAMME_1_ID));
    programmeMembership.setProgrammeMembershipType(ALL_PROGRAMME_MEMBERSHIP_TYPE);
    programmeMembership.setProgrammeStartDate(ALL_PROGRAMME_START_DATE);
    programmeMembership.setProgrammeEndDate(ALL_PROGRAMME_END_DATE);

    CurriculumMembership curriculumMembership = new CurriculumMembership();
    curriculumMembership.setTisId(CURRICULUM_MEMBERSHIP_1_ID);
    curriculumMembership.setData(Map.of(DATA_CURRICULUM_ID, CURRICULUM_1_ID,
        DATA_CURRICULUM_START_DATE, CURRICULUM_1_START_DATE.toString(),
        DATA_CURRICULUM_END_DATE, CURRICULUM_1_END_DATE.toString()));
    when(curriculumMembershipService.findByProgrammeMembershipUuid(ALL_TIS_ID)).thenReturn(
        Set.of(curriculumMembership));

    Curriculum curriculum = new Curriculum();
    curriculum.setTisId(CURRICULUM_1_ID);
    curriculum.setData(Map.of(
        CURRICULUM_NAME, CURRICULUM_1_NAME,
        CURRICULUM_SPECIALTY_ID, SPECIALTY_1_ID
    ));
    when(curriculumService.findById(CURRICULUM_1_ID)).thenReturn(Optional.of(curriculum));

    Specialty specialty = new Specialty();
    specialty.setData(Map.of(
        SPECIALTY_NAME, SPECIALTY_1_NAME,
        SPECIALTY_CODE, SPECIALTY_1_CODE,
        SPECIALTY_BLOCK_INDEMNITY, SPECIALTY_1_BLOCK_INDEMNITY
    ));
    when(specialtyService.findById(SPECIALTY_1_ID)).thenReturn(Optional.of(specialty));

    ConditionsOfJoining conditionsOfJoining = new ConditionsOfJoining();
    conditionsOfJoining.setProgrammeMembershipUuid(ALL_TIS_ID);
    conditionsOfJoining.setSignedAt(COJ_SIGNED_AT);
    conditionsOfJoining.setVersion(COJ_VERSION);
    when(conditionsOfJoiningService.findById(ALL_TIS_ID))
        .thenReturn(Optional.of(conditionsOfJoining));

    Programme programme = new Programme();
    programme.setTisId(PROGRAMME_1_ID);
    programme.setData(Map.of(
        PROGRAMME_NAME, PROGRAMME_1_NAME,
        PROGRAMME_NUMBER, PROGRAMME_1_NUMBER,
        PROGRAMME_OWNER, PROGRAMME_1_OWNER
    ));
    when(programmeService.findById(PROGRAMME_1_ID)).thenReturn(Optional.of(programme));

    LocalOffice localOffice = new LocalOffice();
    localOffice.setData(Map.of(
        LOCAL_OFFICE_NAME, PROGRAMME_1_OWNER
    ));
    when(localOfficeService.findByName(PROGRAMME_1_OWNER)).thenReturn(Optional.of(localOffice));

    enricher.enrich(programmeMembership);

    verify(localOfficeService, never()).requestByName(anyString());
    verify(dbcService, never()).requestByAbbr(anyString());

    ArgumentCaptor<Record> recordCaptor = ArgumentCaptor.forClass(Record.class);
    verify(tcsSyncService).syncRecord(recordCaptor.capture());

    Map<String, String> programmeMembershipData = recordCaptor.getValue().getData();
    assertThat("Unexpected designated body.",
        programmeMembershipData.get(PROGRAMME_MEMBERSHIP_DATA_DESIGNATED_BODY),
        is(nullValue()));
  }

  @Test
  void shouldNotBroadcastCojIfAggregatePmCannotBeBuilt() {
    ProgrammeMembership programmeMembership = new ProgrammeMembership();
    programmeMembership.setUuid(UUID.fromString(ALL_TIS_ID));

    enricher.broadcastCoj(programmeMembership);

    verifyNoInteractions(tcsSyncService);
  }

  @Test
  void shouldBroadcastCojIfAggregatePmCanBeBuilt() {
    ProgrammeMembership programmeMembership = new ProgrammeMembership();
    programmeMembership.setUuid(UUID.fromString(ALL_TIS_ID));
    programmeMembership.setProgrammeId(Long.valueOf(PROGRAMME_1_ID));

    CurriculumMembership curriculumMembership = new CurriculumMembership();
    curriculumMembership.setData(Map.of(DATA_CURRICULUM_ID, CURRICULUM_1_ID));
    when(curriculumMembershipService.findByProgrammeMembershipUuid(ALL_TIS_ID)).thenReturn(
        Collections.singleton(curriculumMembership));

    Curriculum curriculum = new Curriculum();
    curriculum.setData(Map.of(
        CURRICULUM_SPECIALTY_ID, SPECIALTY_1_ID
    ));
    when(curriculumService.findById(CURRICULUM_1_ID)).thenReturn(Optional.of(curriculum));
    when(specialtyService.findById(SPECIALTY_1_ID)).thenReturn(Optional.of(new Specialty()));
    when(programmeService.findById(PROGRAMME_1_ID)).thenReturn(Optional.of(new Programme()));

    enricher.broadcastCoj(programmeMembership);

    verify(tcsSyncService).publishDetailsChangeEvent(any(ProgrammeMembershipEventDto.class));
  }

  /**
   * Helper function to build an enrichable programme membership and, as a side-effect, set
   * necessary mock responses.
   *
   * @return The programme membership.
   */
  ProgrammeMembership buildEnrichableProgrammeMembershipWithAllMocksEnabled() {
    ProgrammeMembership programmeMembership = new ProgrammeMembership();
    programmeMembership.setUuid(UUID.fromString(ALL_TIS_ID));
    programmeMembership.setPersonId(Long.parseLong(ALL_PERSON_ID));
    programmeMembership.setProgrammeId(Long.parseLong(PROGRAMME_1_ID));
    programmeMembership.setProgrammeMembershipType(ALL_PROGRAMME_MEMBERSHIP_TYPE);
    programmeMembership.setProgrammeStartDate(ALL_PROGRAMME_START_DATE);
    programmeMembership.setProgrammeEndDate(ALL_PROGRAMME_END_DATE);

    CurriculumMembership curriculumMembership = new CurriculumMembership();
    curriculumMembership.setTisId(CURRICULUM_MEMBERSHIP_1_ID);
    curriculumMembership.setData(Map.of(DATA_CURRICULUM_ID, CURRICULUM_1_ID,
        DATA_CURRICULUM_START_DATE, CURRICULUM_1_START_DATE.toString(),
        DATA_CURRICULUM_END_DATE, CURRICULUM_1_END_DATE.toString()));
    when(curriculumMembershipService.findByProgrammeMembershipUuid(ALL_TIS_ID)).thenReturn(
        Set.of(curriculumMembership));

    Curriculum curriculum = new Curriculum();
    curriculum.setTisId(CURRICULUM_1_ID);
    curriculum.setData(Map.of(
        CURRICULUM_NAME, CURRICULUM_1_NAME,
        CURRICULUM_SPECIALTY_ID, SPECIALTY_1_ID
    ));
    when(curriculumService.findById(CURRICULUM_1_ID)).thenReturn(Optional.of(curriculum));

    Specialty specialty = new Specialty();
    specialty.setData(Map.of(
        SPECIALTY_NAME, SPECIALTY_1_NAME,
        SPECIALTY_CODE, SPECIALTY_1_CODE,
        SPECIALTY_BLOCK_INDEMNITY, SPECIALTY_1_BLOCK_INDEMNITY
    ));
    when(specialtyService.findById(SPECIALTY_1_ID)).thenReturn(Optional.of(specialty));

    ConditionsOfJoining conditionsOfJoining = new ConditionsOfJoining();
    conditionsOfJoining.setProgrammeMembershipUuid(ALL_TIS_ID);
    conditionsOfJoining.setSignedAt(COJ_SIGNED_AT);
    conditionsOfJoining.setVersion(COJ_VERSION);
    when(conditionsOfJoiningService.findById(ALL_TIS_ID))
        .thenReturn(Optional.of(conditionsOfJoining));

    Programme programme = new Programme();
    programme.setTisId(PROGRAMME_1_ID);
    programme.setData(Map.of(
        PROGRAMME_NAME, PROGRAMME_1_NAME,
        PROGRAMME_NUMBER, PROGRAMME_1_NUMBER,
        PROGRAMME_OWNER, PROGRAMME_1_OWNER
    ));
    when(programmeService.findById(PROGRAMME_1_ID)).thenReturn(Optional.of(programme));

    LocalOffice localOffice = new LocalOffice();
    localOffice.setData(Map.of(
        LOCAL_OFFICE_NAME, PROGRAMME_1_OWNER,
        LOCAL_OFFICE_ABBREVIATION, LOCAL_OFFICE_ABBREVIATION_VALUE
    ));
    when(localOfficeService.findByName(PROGRAMME_1_OWNER)).thenReturn(Optional.of(localOffice));

    Dbc dbc = new Dbc();
    dbc.setData(Map.of(
        DBC_ABBR, LOCAL_OFFICE_ABBREVIATION_VALUE,
        DBC_NAME, DBC_NAME_VALUE,
        DBC_DBC, DBC_DBC_VALUE
    ));
    when(dbcService.findByAbbr(LOCAL_OFFICE_ABBREVIATION_VALUE)).thenReturn(Optional.of(dbc));

    UserDesignatedBody udb = new UserDesignatedBody();
    udb.setData(Map.of(
        USER_DB_USER_NAME, USER_NAME_VALUE,
        USER_DB_DBC, DBC_DBC_VALUE
    ));
    when(udbService.findByDbc(DBC_DBC_VALUE)).thenReturn(Set.of(udb));

    UserRole userRole = new UserRole();
    userRole.setData(Map.of(
        USER_ROLE_USER_NAME, USER_NAME_VALUE,
        USER_ROLE_ROLE_NAME, ROLE_VALUE
    ));
    when(userRoleService.findRvOfficerRoleByUserName(USER_NAME_VALUE)).thenReturn(
        Optional.of(userRole));

    HeeUser heeUser = new HeeUser();
    heeUser.setData(Map.of(
        HEE_USER_NAME, USER_NAME_VALUE,
        HEE_USER_FIRST_NAME, USER_FIRST_NAME_VALUE,
        HEE_USER_LAST_NAME, USER_LAST_NAME_VALUE,
        HEE_USER_EMAIL, USER_EMAIL_VALUE,
        HEE_USER_GMC, USER_GMC_VALUE,
        HEE_USER_PHONE, USER_PHONE_VALUE
    ));
    when(heeUserService.findByName(USER_NAME_VALUE)).thenReturn(Optional.of(heeUser));

    return programmeMembership;
  }
}
