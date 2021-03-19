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
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.internal.util.collections.Sets;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.nhs.hee.tis.trainee.sync.model.Curriculum;
import uk.nhs.hee.tis.trainee.sync.model.Programme;
import uk.nhs.hee.tis.trainee.sync.model.ProgrammeMembership;
import uk.nhs.hee.tis.trainee.sync.service.CurriculumSyncService;
import uk.nhs.hee.tis.trainee.sync.service.ProgrammeMembershipSyncService;
import uk.nhs.hee.tis.trainee.sync.service.ProgrammeSyncService;
import uk.nhs.hee.tis.trainee.sync.service.TcsSyncService;

@ExtendWith(MockitoExtension.class)
class ProgrammeMembershipEnricherFacadeTest {

  private static final String PROGRAMME_MEMBERSHIP_A11_TIS_ID = "11";
  private static final String PROGRAMME_MEMBERSHIP_A11_PROGRAMME_ID = "programme1";
  private static final String PROGRAMME_MEMBERSHIP_A11_CURRICULUM_ID = "curriculum1";
  private static final String PROGRAMME_MEMBERSHIP_A11_PROGRAMME_COMPLETION_DATE = "2020-01-31";

  private static final String PROGRAMME_MEMBERSHIP_A12_TIS_ID = "12";
  private static final String PROGRAMME_MEMBERSHIP_A12_PROGRAMME_ID = "programme1";
  private static final String PROGRAMME_MEMBERSHIP_A12_CURRICULUM_ID = "curriculum2";
  private static final String PROGRAMME_MEMBERSHIP_A12_PROGRAMME_COMPLETION_DATE = "2020-06-30";

  private static final String PROGRAMME_MEMBERSHIP_A32_TIS_ID = "32";
  private static final String PROGRAMME_MEMBERSHIP_A32_PROGRAMME_ID = "programme3";
  private static final String PROGRAMME_MEMBERSHIP_A32_CURRICULUM_ID = "curriculum2";
  private static final String PROGRAMME_MEMBERSHIP_A32_PROGRAMME_COMPLETION_DATE = "2020-06-30";

  private static final String PROGRAMME_1_ID = "programme1";
  private static final String PROGRAMME_1_NAME = "programme One";
  private static final String PROGRAMME_1_NAME_UPDATED = "programme One updated";
  private static final String PROGRAMME_1_NUMBER = "programme No. One";
  private static final String PROGRAMME_1_OWNER = "programme One owner";
  private static final String PROGRAMME_3_ID = "programme3";
  private static final String PROGRAMME_3_NAME = "programme Three";
  private static final String CURRICULUM_1_ID = "curriculum1";
  private static final String CURRICULUM_1_NAME = "curriculum One";
  private static final String CURRICULUM_2_ID = "curriculum2";
  private static final String CURRICULUM_2_NAME = "curriculum Two";
  private static final String CURRICULUM_2_NAME_UPDATED = "curriculum Two updated";
  private static final String ALL_TIS_ID = "1";
  private static final String ALL_PERSON_ID = "personA";
  private static final String ALL_PROGRAMME_COMPLETION_DATE = "2020-02-01";
  private static final String ALL_PROGRAMME_MEMBERSHIP_TYPE = "SUBSTANTIVE";
  private static final String ALL_PROGRAMME_START_DATE = "2020-01-01";
  private static final String ALL_PROGRAMME_END_DATE = "2021-01-01";

  //fields in curriculum/programme sync repo documents
  private static final String CURRICULUM_NAME = "name";
  private static final String PROGRAMME_NAME = "programmeName";
  private static final String PROGRAMME_NUMBER = "programmeNumber";
  private static final String PROGRAMME_OWNER = "owner";

  // fields in programmeMembership sync repo documents
  private static final String DATA_TIS_ID = "tisId";
  private static final String DATA_PROGRAMME_ID = "programmeId";
  private static final String DATA_CURRICULUM_ID = "curriculumId";
  private static final String DATA_PERSON_ID = "personId";
  private static final String DATA_PROGRAMME_MEMBERSHIP_TYPE = "programmeMembershipType";
  private static final String DATA_PROGRAMME_END_DATE = "programmeEndDate";
  private static final String DATA_PROGRAMME_START_DATE = "programmeStartDate";
  private static final String DATA_PROGRAMME_COMPLETION_DATE = "programmeCompletionDate";
  private static final String DATA_PROGRAMME_NUMBER = "programmeNumber";
  private static final String DATA_MANAGING_DEANERY = "managingDeanery";

  // processed fields in programmeMembership DTO passed to trainee-details for persisting
  private static final String PROGRAMME_MEMBERSHIP_DATA_PROGRAMME_NAME = "programmeName";
  private static final String PROGRAMME_MEMBERSHIP_DATA_PROGRAMME_NUMBER = "programmeNumber";
  private static final String PROGRAMME_MEMBERSHIP_DATA_MANAGING_DEANERY = "managingDeanery";
  private static final String PROGRAMME_MEMBERSHIP_DATA_CURRICULA = "curricula";
  private static final String PROGRAMME_MEMBERSHIP_DATA_CURRICULUM_NAME = "curriculumName";
  private static final String PROGRAMME_MEMBERSHIP_DATA_PROGRAMME_COMPLETION_DATE =
      "programmeCompletionDate";

  @InjectMocks
  @Spy
  private ProgrammeMembershipEnricherFacade enricher;

  @Mock
  private ProgrammeMembershipSyncService programmeMembershipService;

  @Mock
  private ProgrammeSyncService programmeService;

  @Mock
  private CurriculumSyncService curriculumService;

  @Mock
  private TcsSyncService tcsSyncService;

  @Test
  void shouldEnrichFromProgrammeMembershipWhenProgrammeAndCurriculumExist() {
    ProgrammeMembership programmeMembership = new ProgrammeMembership();
    programmeMembership.setData(new HashMap<>(Map.of(
        DATA_TIS_ID, ALL_TIS_ID,
        DATA_PERSON_ID, ALL_PERSON_ID,
        DATA_PROGRAMME_ID, PROGRAMME_1_ID,
        DATA_CURRICULUM_ID, CURRICULUM_1_ID,
        DATA_PROGRAMME_MEMBERSHIP_TYPE, ALL_PROGRAMME_MEMBERSHIP_TYPE,
        DATA_PROGRAMME_START_DATE, ALL_PROGRAMME_START_DATE,
        DATA_PROGRAMME_END_DATE, ALL_PROGRAMME_END_DATE)));
    programmeMembership.setTisId(ALL_TIS_ID);

    Programme programme = new Programme();
    programme.setTisId(PROGRAMME_1_ID);
    programme.setData(Map.of(
        PROGRAMME_NAME, PROGRAMME_1_NAME,
        PROGRAMME_NUMBER, PROGRAMME_1_NUMBER,
        PROGRAMME_OWNER, PROGRAMME_1_OWNER
    ));
    Curriculum curriculum = new Curriculum();
    curriculum.setTisId(CURRICULUM_1_ID);
    curriculum.setData(Map.of(
        CURRICULUM_NAME, CURRICULUM_1_NAME
    ));

    when(programmeService.findById(PROGRAMME_1_ID)).thenReturn(Optional.of(programme));
    when(curriculumService.findById(CURRICULUM_1_ID)).thenReturn(Optional.of(curriculum));
    when(programmeMembershipService.findBySimilar(ALL_PERSON_ID, PROGRAMME_1_ID,
        ALL_PROGRAMME_MEMBERSHIP_TYPE, ALL_PROGRAMME_START_DATE, ALL_PROGRAMME_END_DATE))
        .thenReturn(java.util.Collections.singleton(programmeMembership));

    enricher.enrich(programmeMembership);

    verify(programmeMembershipService, never()).request(anyString());
    verify(programmeService, never()).request(anyString());
    verify(curriculumService, never()).request(anyString());

    tcsSyncService.syncRecord(programmeMembership);

    Map<String, String> programmeMembershipData = programmeMembership.getData();
    assertThat("Unexpected programme name.",
        programmeMembershipData.get(PROGRAMME_MEMBERSHIP_DATA_PROGRAMME_NAME),
        is(PROGRAMME_1_NAME));
    assertThat("Unexpected programme number.",
        programmeMembershipData.get(PROGRAMME_MEMBERSHIP_DATA_PROGRAMME_NUMBER),
        is(PROGRAMME_1_NUMBER));
    assertThat("Unexpected managing deanery.",
        programmeMembershipData.get(PROGRAMME_MEMBERSHIP_DATA_MANAGING_DEANERY),
        is(PROGRAMME_1_OWNER));
    Set<Map<String,String>> programmeMembershipCurricula = getCurriculaFromJson(
        programmeMembershipData.get(PROGRAMME_MEMBERSHIP_DATA_CURRICULA));
    assertThat("Unexpected curricula size.", programmeMembershipCurricula.size(),
        is(1));
    assertThat("Unexpected curriculum name.",
        programmeMembershipCurricula.iterator().next()
            .get(PROGRAMME_MEMBERSHIP_DATA_CURRICULUM_NAME),
        is(CURRICULUM_1_NAME));
  }

  @Test
  void shouldEnrichPmCurriculaAndProgrammeCompletionDateFromAllSimilarPms() {
    // the programme membership to enrich
    ProgrammeMembership programmeMembership = new ProgrammeMembership();
    programmeMembership.setData(new HashMap<>(Map.of(
        DATA_TIS_ID, ALL_TIS_ID,
        DATA_PERSON_ID, ALL_PERSON_ID,
        DATA_PROGRAMME_ID, PROGRAMME_1_ID,
        DATA_CURRICULUM_ID, CURRICULUM_1_ID,
        DATA_PROGRAMME_MEMBERSHIP_TYPE, ALL_PROGRAMME_MEMBERSHIP_TYPE,
        DATA_PROGRAMME_START_DATE, ALL_PROGRAMME_START_DATE,
        DATA_PROGRAMME_END_DATE, ALL_PROGRAMME_END_DATE,
        DATA_PROGRAMME_COMPLETION_DATE, ALL_PROGRAMME_COMPLETION_DATE)));
    programmeMembership.setTisId(ALL_TIS_ID);

    // similar programme memberships
    ProgrammeMembership programmeMembership1 = new ProgrammeMembership();
    programmeMembership1.setData(new HashMap<>(Map.of(
        DATA_TIS_ID, PROGRAMME_MEMBERSHIP_A11_TIS_ID,
        DATA_PERSON_ID, ALL_PERSON_ID,
        DATA_PROGRAMME_ID, PROGRAMME_MEMBERSHIP_A11_PROGRAMME_ID,
        DATA_CURRICULUM_ID, PROGRAMME_MEMBERSHIP_A11_CURRICULUM_ID,
        DATA_PROGRAMME_MEMBERSHIP_TYPE, ALL_PROGRAMME_MEMBERSHIP_TYPE,
        DATA_PROGRAMME_START_DATE, ALL_PROGRAMME_START_DATE,
        DATA_PROGRAMME_END_DATE, ALL_PROGRAMME_END_DATE,
        DATA_PROGRAMME_COMPLETION_DATE, PROGRAMME_MEMBERSHIP_A11_PROGRAMME_COMPLETION_DATE)));
    programmeMembership1.setTisId(PROGRAMME_MEMBERSHIP_A11_TIS_ID);
    ProgrammeMembership programmeMembership2 = new ProgrammeMembership();
    programmeMembership2.setData(new HashMap<>(Map.of(
        DATA_TIS_ID, PROGRAMME_MEMBERSHIP_A12_TIS_ID,
        DATA_PERSON_ID, ALL_PERSON_ID,
        DATA_PROGRAMME_ID, PROGRAMME_MEMBERSHIP_A12_PROGRAMME_ID,
        DATA_CURRICULUM_ID, PROGRAMME_MEMBERSHIP_A12_CURRICULUM_ID,
        DATA_PROGRAMME_MEMBERSHIP_TYPE, ALL_PROGRAMME_MEMBERSHIP_TYPE,
        DATA_PROGRAMME_START_DATE, ALL_PROGRAMME_START_DATE,
        DATA_PROGRAMME_END_DATE, ALL_PROGRAMME_END_DATE,
        DATA_PROGRAMME_COMPLETION_DATE, PROGRAMME_MEMBERSHIP_A12_PROGRAMME_COMPLETION_DATE)));
    programmeMembership2.setTisId(PROGRAMME_MEMBERSHIP_A12_TIS_ID);

    Programme programme = new Programme();
    programme.setTisId(PROGRAMME_1_ID);
    programme.setData(Map.of(
        PROGRAMME_NAME, PROGRAMME_1_NAME
    ));
    Curriculum curriculum1 = new Curriculum();
    curriculum1.setTisId(CURRICULUM_1_ID);
    curriculum1.setData(Map.of(
        CURRICULUM_NAME, CURRICULUM_1_NAME
    ));
    Curriculum curriculum2 = new Curriculum();
    curriculum2.setTisId(CURRICULUM_2_ID);
    curriculum2.setData(Map.of(
        CURRICULUM_NAME, CURRICULUM_2_NAME
    ));

    when(programmeService.findById(PROGRAMME_1_ID)).thenReturn(Optional.of(programme));
    when(curriculumService.findById(CURRICULUM_1_ID)).thenReturn(Optional.of(curriculum1));
    when(curriculumService.findById(CURRICULUM_2_ID)).thenReturn(Optional.of(curriculum2));
    when(programmeMembershipService.findBySimilar(ALL_PERSON_ID, PROGRAMME_1_ID,
        ALL_PROGRAMME_MEMBERSHIP_TYPE, ALL_PROGRAMME_START_DATE, ALL_PROGRAMME_END_DATE))
        .thenReturn(Sets.newSet(programmeMembership1, programmeMembership2));

    enricher.enrich(programmeMembership);

    verify(programmeMembershipService, never()).request(anyString());
    verify(programmeService, never()).request(anyString());
    verify(curriculumService, never()).request(anyString());

    tcsSyncService.syncRecord(programmeMembership);

    Map<String, String> programmeMembershipData = programmeMembership.getData();
    assertThat("Unexpected programme name.",
        programmeMembershipData.get(PROGRAMME_MEMBERSHIP_DATA_PROGRAMME_NAME),
        is(PROGRAMME_1_NAME));
    assertThat("Unexpected programme completion date.",
        programmeMembershipData.get(PROGRAMME_MEMBERSHIP_DATA_PROGRAMME_COMPLETION_DATE),
        is(PROGRAMME_MEMBERSHIP_A12_PROGRAMME_COMPLETION_DATE));

    Set<Map<String,String>> programmeMembershipCurricula = getCurriculaFromJson(
        programmeMembershipData.get(PROGRAMME_MEMBERSHIP_DATA_CURRICULA));
    assertThat("Unexpected curricula size.", programmeMembershipCurricula.size(),
        is(2)); // not 3, since curriculum 1 is represented twice

    List<String> curriculaNames = new ArrayList<>();
    Iterator<Map<String,String>> it = programmeMembershipCurricula.iterator();
    while (it.hasNext()) {
      curriculaNames.add(it.next().get(PROGRAMME_MEMBERSHIP_DATA_CURRICULUM_NAME));
    }
    assertThat("Unexpected curriculum name.", curriculaNames.contains(CURRICULUM_1_NAME),
        is(true));
    assertThat("Unexpected curriculum name.", curriculaNames.contains(CURRICULUM_2_NAME),
        is(true));
  }

  @Test
  void shouldEnrichProgrammeMembershipsFromProgramme() {
    ProgrammeMembership programmeMembership1 = new ProgrammeMembership();
    programmeMembership1.setData(new HashMap<>(Map.of(
        DATA_TIS_ID, PROGRAMME_MEMBERSHIP_A11_TIS_ID,
        DATA_PERSON_ID, ALL_PERSON_ID,
        DATA_PROGRAMME_ID, PROGRAMME_MEMBERSHIP_A11_PROGRAMME_ID,
        DATA_CURRICULUM_ID, PROGRAMME_MEMBERSHIP_A11_CURRICULUM_ID,
        DATA_PROGRAMME_MEMBERSHIP_TYPE, ALL_PROGRAMME_MEMBERSHIP_TYPE,
        DATA_PROGRAMME_START_DATE, ALL_PROGRAMME_START_DATE,
        DATA_PROGRAMME_END_DATE, ALL_PROGRAMME_END_DATE,
        DATA_PROGRAMME_COMPLETION_DATE, PROGRAMME_MEMBERSHIP_A11_PROGRAMME_COMPLETION_DATE)));
    programmeMembership1.setTisId(PROGRAMME_MEMBERSHIP_A11_TIS_ID);
    ProgrammeMembership programmeMembership2 = new ProgrammeMembership();
    programmeMembership2.setData(new HashMap<>(Map.of(
        DATA_TIS_ID, PROGRAMME_MEMBERSHIP_A12_TIS_ID,
        DATA_PERSON_ID, ALL_PERSON_ID,
        DATA_PROGRAMME_ID, PROGRAMME_MEMBERSHIP_A12_PROGRAMME_ID,
        DATA_CURRICULUM_ID, PROGRAMME_MEMBERSHIP_A12_CURRICULUM_ID,
        DATA_PROGRAMME_MEMBERSHIP_TYPE, ALL_PROGRAMME_MEMBERSHIP_TYPE,
        DATA_PROGRAMME_START_DATE, ALL_PROGRAMME_START_DATE,
        DATA_PROGRAMME_END_DATE, ALL_PROGRAMME_END_DATE,
        DATA_PROGRAMME_COMPLETION_DATE, PROGRAMME_MEMBERSHIP_A12_PROGRAMME_COMPLETION_DATE)));
    programmeMembership2.setTisId(PROGRAMME_MEMBERSHIP_A12_TIS_ID);

    Programme programme = new Programme();
    programme.setTisId(PROGRAMME_1_ID);
    programme.setData(Map.of(
        PROGRAMME_NAME, PROGRAMME_1_NAME_UPDATED
    ));
    Curriculum curriculum1 = new Curriculum();
    curriculum1.setTisId(CURRICULUM_1_ID);
    curriculum1.setData(Map.of(
        CURRICULUM_NAME, CURRICULUM_1_NAME
    ));
    Curriculum curriculum2 = new Curriculum();
    curriculum2.setTisId(CURRICULUM_2_ID);
    curriculum2.setData(Map.of(
        CURRICULUM_NAME, CURRICULUM_2_NAME
    ));

    when(curriculumService.findById(CURRICULUM_1_ID))
        .thenReturn(Optional.of(curriculum1));
    when(curriculumService.findById(CURRICULUM_2_ID))
        .thenReturn(Optional.of(curriculum2));
    when(programmeMembershipService.findByProgrammeId(PROGRAMME_1_ID))
        .thenReturn(Sets.newSet(programmeMembership1, programmeMembership2));
    when(programmeMembershipService.findBySimilar(ALL_PERSON_ID, PROGRAMME_1_ID,
        ALL_PROGRAMME_MEMBERSHIP_TYPE, ALL_PROGRAMME_START_DATE, ALL_PROGRAMME_END_DATE))
        .thenReturn(Sets.newSet(programmeMembership1, programmeMembership2));

    enricher.enrich(programme);

    verify(programmeMembershipService, never()).request(anyString());
    verify(programmeService, never()).request(anyString());
    verify(curriculumService, never()).request(anyString());

    tcsSyncService.syncRecord(programmeMembership1);
    tcsSyncService.syncRecord(programmeMembership2);

    Map<String, String> programmeMembership1Data = programmeMembership1.getData();
    assertThat("Unexpected programme name.",
        programmeMembership1Data.get(PROGRAMME_MEMBERSHIP_DATA_PROGRAMME_NAME),
        is(PROGRAMME_1_NAME_UPDATED));
    Map<String, String> programmeMembership2Data = programmeMembership2.getData();
    assertThat("Unexpected programme name.",
        programmeMembership2Data.get(PROGRAMME_MEMBERSHIP_DATA_PROGRAMME_NAME),
        is(PROGRAMME_1_NAME_UPDATED));
  }

  @Test
  void shouldEnrichProgrammeMembershipsFromCurriculum() {
    ProgrammeMembership programmeMembership1 = new ProgrammeMembership();
    programmeMembership1.setData(new HashMap<>(Map.of(
        DATA_TIS_ID, PROGRAMME_MEMBERSHIP_A12_TIS_ID,
        DATA_PERSON_ID, ALL_PERSON_ID,
        DATA_PROGRAMME_ID, PROGRAMME_MEMBERSHIP_A12_PROGRAMME_ID,
        DATA_CURRICULUM_ID, PROGRAMME_MEMBERSHIP_A12_CURRICULUM_ID,
        DATA_PROGRAMME_MEMBERSHIP_TYPE, ALL_PROGRAMME_MEMBERSHIP_TYPE,
        DATA_PROGRAMME_START_DATE, ALL_PROGRAMME_START_DATE,
        DATA_PROGRAMME_END_DATE, ALL_PROGRAMME_END_DATE,
        DATA_PROGRAMME_COMPLETION_DATE, PROGRAMME_MEMBERSHIP_A12_PROGRAMME_COMPLETION_DATE)));
    programmeMembership1.setTisId(PROGRAMME_MEMBERSHIP_A12_TIS_ID);
    ProgrammeMembership programmeMembership2 = new ProgrammeMembership();
    programmeMembership2.setData(new HashMap<>(Map.of(
        DATA_TIS_ID, PROGRAMME_MEMBERSHIP_A32_TIS_ID,
        DATA_PERSON_ID, ALL_PERSON_ID,
        DATA_PROGRAMME_ID, PROGRAMME_MEMBERSHIP_A32_PROGRAMME_ID,
        DATA_CURRICULUM_ID, PROGRAMME_MEMBERSHIP_A32_CURRICULUM_ID,
        DATA_PROGRAMME_MEMBERSHIP_TYPE, ALL_PROGRAMME_MEMBERSHIP_TYPE,
        DATA_PROGRAMME_START_DATE, ALL_PROGRAMME_START_DATE,
        DATA_PROGRAMME_END_DATE, ALL_PROGRAMME_END_DATE,
        DATA_PROGRAMME_COMPLETION_DATE, PROGRAMME_MEMBERSHIP_A32_PROGRAMME_COMPLETION_DATE)));
    programmeMembership1.setTisId(PROGRAMME_MEMBERSHIP_A32_TIS_ID);

    Programme programme1 = new Programme();
    programme1.setTisId(PROGRAMME_1_ID);
    programme1.setData(Map.of(
        PROGRAMME_NAME, PROGRAMME_1_NAME
    ));
    Programme programme3 = new Programme();
    programme3.setTisId(PROGRAMME_3_ID);
    programme3.setData(Map.of(
        PROGRAMME_NAME, PROGRAMME_3_NAME
    ));
    Curriculum curriculum = new Curriculum();
    curriculum.setTisId(CURRICULUM_2_ID);
    curriculum.setData(Map.of(
        CURRICULUM_NAME, CURRICULUM_2_NAME_UPDATED
    ));

    when(programmeService.findById(PROGRAMME_1_ID)).thenReturn(Optional.of(programme1));
    when(programmeService.findById(PROGRAMME_3_ID)).thenReturn(Optional.of(programme3));
    when(curriculumService.findById(CURRICULUM_2_ID)).thenReturn(Optional.of(curriculum));
    when(programmeMembershipService.findByCurriculumId(CURRICULUM_2_ID))
        .thenReturn(Sets.newSet(programmeMembership1, programmeMembership2));
    when(programmeMembershipService.findBySimilar(ALL_PERSON_ID, PROGRAMME_1_ID,
        ALL_PROGRAMME_MEMBERSHIP_TYPE, ALL_PROGRAMME_START_DATE, ALL_PROGRAMME_END_DATE))
        .thenReturn(Sets.newSet(programmeMembership1));
    when(programmeMembershipService.findBySimilar(ALL_PERSON_ID, PROGRAMME_3_ID,
        ALL_PROGRAMME_MEMBERSHIP_TYPE, ALL_PROGRAMME_START_DATE, ALL_PROGRAMME_END_DATE))
        .thenReturn(Sets.newSet(programmeMembership2));

    enricher.enrich(curriculum);

    verify(programmeMembershipService, never()).request(anyString());
    verify(programmeService, never()).request(anyString());
    verify(curriculumService, never()).request(anyString());

    tcsSyncService.syncRecord(programmeMembership1);
    tcsSyncService.syncRecord(programmeMembership2);

    Map<String, String> programmeMembership1Data = programmeMembership1.getData();
    Set<Map<String,String>> programmeMembership1Curricula =
        getCurriculaFromJson(programmeMembership1Data.get(PROGRAMME_MEMBERSHIP_DATA_CURRICULA));
    assertThat("Unexpected curricula size.", programmeMembership1Curricula.size(),
        is(1));
    assertThat("Unexpected curriculum name.",
        programmeMembership1Curricula.iterator().next()
            .get(PROGRAMME_MEMBERSHIP_DATA_CURRICULUM_NAME),
        is(CURRICULUM_2_NAME_UPDATED));

    Map<String, String> programmeMembership2Data = programmeMembership2.getData();
    Set<Map<String,String>> programmeMembership2Curricula =
        getCurriculaFromJson(programmeMembership2Data.get(PROGRAMME_MEMBERSHIP_DATA_CURRICULA));
    assertThat("Unexpected curricula size.", programmeMembership2Curricula.size(),
        is(1));
    assertThat("Unexpected curriculum name.",
        programmeMembership2Curricula.iterator().next()
            .get(PROGRAMME_MEMBERSHIP_DATA_CURRICULUM_NAME),
        is(CURRICULUM_2_NAME_UPDATED));
  }

  @Test
  void shouldNotEnrichFromProgrammeMembershipWhenCurriculumNotExist() {
    ProgrammeMembership programmeMembership = new ProgrammeMembership();
    programmeMembership.setData(new HashMap<>(Map.of(
        DATA_TIS_ID, ALL_TIS_ID,
        DATA_PERSON_ID, ALL_PERSON_ID,
        DATA_PROGRAMME_ID, PROGRAMME_1_ID,
        DATA_CURRICULUM_ID, CURRICULUM_1_ID,
        DATA_PROGRAMME_MEMBERSHIP_TYPE, ALL_PROGRAMME_MEMBERSHIP_TYPE,
        DATA_PROGRAMME_START_DATE, ALL_PROGRAMME_START_DATE,
        DATA_PROGRAMME_END_DATE, ALL_PROGRAMME_END_DATE)));
    programmeMembership.setTisId(ALL_TIS_ID);

    Programme programme = new Programme();
    programme.setTisId(PROGRAMME_1_ID);
    programme.setData(Map.of(
        PROGRAMME_NAME, PROGRAMME_1_NAME
    ));

    when(programmeService.findById(PROGRAMME_1_ID)).thenReturn(Optional.of(programme));
    when(curriculumService.findById(CURRICULUM_1_ID)).thenReturn(Optional.empty());

    enricher.enrich(programmeMembership);

    verify(programmeMembershipService, never()).request(anyString());
    verify(programmeService).findById(PROGRAMME_1_ID);
    verify(programmeService, never()).request(anyString());
    verify(curriculumService).findById(CURRICULUM_1_ID);
    verify(curriculumService).request(CURRICULUM_1_ID);

    verifyNoInteractions(tcsSyncService);
  }

  @Test
  void shouldNotEnrichFromProgrammeMembershipWhenProgrammeNotExist() {
    ProgrammeMembership programmeMembership = new ProgrammeMembership();
    programmeMembership.setData(new HashMap<>(Map.of(
        DATA_TIS_ID, ALL_TIS_ID,
        DATA_PERSON_ID, ALL_PERSON_ID,
        DATA_PROGRAMME_ID, PROGRAMME_1_ID,
        DATA_CURRICULUM_ID, CURRICULUM_1_ID,
        DATA_PROGRAMME_MEMBERSHIP_TYPE, ALL_PROGRAMME_MEMBERSHIP_TYPE,
        DATA_PROGRAMME_START_DATE, ALL_PROGRAMME_START_DATE,
        DATA_PROGRAMME_END_DATE, ALL_PROGRAMME_END_DATE)));
    programmeMembership.setTisId(ALL_TIS_ID);

    Curriculum curriculum = new Curriculum();
    curriculum.setTisId(CURRICULUM_1_ID);
    curriculum.setData(Map.of(
        CURRICULUM_NAME, CURRICULUM_1_NAME
    ));

    when(programmeService.findById(PROGRAMME_1_ID)).thenReturn(Optional.empty());
    when(curriculumService.findById(CURRICULUM_1_ID)).thenReturn(Optional.of(curriculum));

    enricher.enrich(programmeMembership);

    verify(programmeMembershipService, never()).request(anyString());
    verify(programmeService).findById(PROGRAMME_1_ID);
    verify(programmeService).request(PROGRAMME_1_ID);
    verify(curriculumService).findById(CURRICULUM_1_ID);
    verify(curriculumService, never()).request(anyString());

    verifyNoInteractions(tcsSyncService);
  }

  @Test
  void shouldNotEnrichFromProgrammeWhenSomeCurriculaNotExist() {
    ProgrammeMembership programmeMembership1 = new ProgrammeMembership();
    programmeMembership1.setData(new HashMap<>(Map.of(
        DATA_TIS_ID, PROGRAMME_MEMBERSHIP_A11_TIS_ID,
        DATA_PERSON_ID, ALL_PERSON_ID,
        DATA_PROGRAMME_ID, PROGRAMME_MEMBERSHIP_A11_PROGRAMME_ID,
        DATA_CURRICULUM_ID, PROGRAMME_MEMBERSHIP_A11_CURRICULUM_ID,
        DATA_PROGRAMME_MEMBERSHIP_TYPE, ALL_PROGRAMME_MEMBERSHIP_TYPE,
        DATA_PROGRAMME_START_DATE, ALL_PROGRAMME_START_DATE,
        DATA_PROGRAMME_END_DATE, ALL_PROGRAMME_END_DATE,
        DATA_PROGRAMME_COMPLETION_DATE, PROGRAMME_MEMBERSHIP_A11_PROGRAMME_COMPLETION_DATE)));
    programmeMembership1.setTisId(PROGRAMME_MEMBERSHIP_A11_TIS_ID);
    ProgrammeMembership programmeMembership2 = new ProgrammeMembership();
    programmeMembership2.setData(new HashMap<>(Map.of(
        DATA_TIS_ID, PROGRAMME_MEMBERSHIP_A12_TIS_ID,
        DATA_PERSON_ID, ALL_PERSON_ID,
        DATA_PROGRAMME_ID, PROGRAMME_MEMBERSHIP_A12_PROGRAMME_ID,
        DATA_CURRICULUM_ID, PROGRAMME_MEMBERSHIP_A12_CURRICULUM_ID,
        DATA_PROGRAMME_MEMBERSHIP_TYPE, ALL_PROGRAMME_MEMBERSHIP_TYPE,
        DATA_PROGRAMME_START_DATE, ALL_PROGRAMME_START_DATE,
        DATA_PROGRAMME_END_DATE, ALL_PROGRAMME_END_DATE,
        DATA_PROGRAMME_COMPLETION_DATE, PROGRAMME_MEMBERSHIP_A12_PROGRAMME_COMPLETION_DATE)));
    programmeMembership2.setTisId(PROGRAMME_MEMBERSHIP_A12_TIS_ID);

    Programme programme = new Programme();
    programme.setTisId(PROGRAMME_1_ID);
    programme.setData(Map.of(
        PROGRAMME_NAME, PROGRAMME_1_NAME_UPDATED
    ));
    Curriculum curriculum1 = new Curriculum();
    curriculum1.setTisId(CURRICULUM_1_ID);
    curriculum1.setData(Map.of(
        CURRICULUM_NAME, CURRICULUM_1_NAME
    ));

    when(curriculumService.findById(CURRICULUM_1_ID)).thenReturn(Optional.of(curriculum1));
    when(curriculumService.findById(CURRICULUM_2_ID)).thenReturn(Optional.empty());
    when(programmeMembershipService.findByProgrammeId(PROGRAMME_1_ID))
        .thenReturn(Sets.newSet(programmeMembership1, programmeMembership2));
    when(programmeMembershipService.findBySimilar(ALL_PERSON_ID, PROGRAMME_1_ID,
        ALL_PROGRAMME_MEMBERSHIP_TYPE, ALL_PROGRAMME_START_DATE, ALL_PROGRAMME_END_DATE))
        .thenReturn(Sets.newSet(programmeMembership1, programmeMembership2));

    enricher.enrich(programme);

    verify(programmeMembershipService, never()).request(anyString());
    verify(programmeService, never()).request(anyString());
    verify(curriculumService, times(2)).findById(CURRICULUM_1_ID);
    verify(curriculumService, times(2)).findById(CURRICULUM_2_ID);
    verify(curriculumService).request(CURRICULUM_2_ID); // should only request once

    verifyNoInteractions(tcsSyncService);
  }

  @Test
  void shouldDeletePersonsProgrammeMembershipsBeforeSyncingProgrammeMembership() {
    ProgrammeMembership programmeMembership = new ProgrammeMembership();
    programmeMembership.setData(new HashMap<>(Map.of(
        DATA_PERSON_ID, ALL_PERSON_ID,
        DATA_PROGRAMME_ID, PROGRAMME_MEMBERSHIP_A11_PROGRAMME_ID,
        DATA_CURRICULUM_ID, PROGRAMME_MEMBERSHIP_A11_CURRICULUM_ID,
        DATA_PROGRAMME_MEMBERSHIP_TYPE, ALL_PROGRAMME_MEMBERSHIP_TYPE,
        DATA_PROGRAMME_START_DATE, ALL_PROGRAMME_START_DATE,
        DATA_PROGRAMME_END_DATE, ALL_PROGRAMME_END_DATE,
        DATA_PROGRAMME_COMPLETION_DATE, PROGRAMME_MEMBERSHIP_A11_PROGRAMME_COMPLETION_DATE)));

    Programme programme = new Programme();
    programme.setTisId(PROGRAMME_1_ID);
    programme.setData(Map.of(
        PROGRAMME_NAME, PROGRAMME_1_NAME
    ));
    Curriculum curriculum = new Curriculum();
    curriculum.setTisId(CURRICULUM_1_ID);
    curriculum.setData(Map.of(
        CURRICULUM_NAME, CURRICULUM_1_NAME
    ));

    when(curriculumService.findById(CURRICULUM_1_ID)).thenReturn(Optional.of(curriculum));
    when(programmeService.findById(PROGRAMME_1_ID)).thenReturn(Optional.of(programme));
    when(programmeMembershipService.findByPersonId(ALL_PERSON_ID))
        .thenReturn(Collections.singleton(programmeMembership));
    when(programmeMembershipService.findBySimilar(ALL_PERSON_ID, PROGRAMME_1_ID,
        ALL_PROGRAMME_MEMBERSHIP_TYPE, ALL_PROGRAMME_START_DATE, ALL_PROGRAMME_END_DATE))
        .thenReturn(Collections.singleton(programmeMembership));

    enricher.enrich(programmeMembership);

    // the initial 'DELETE' and then 'LOAD' sync both use programmeMembership
    verify(tcsSyncService, times(2)).syncRecord(programmeMembership);
  }

  @Test
  void shouldNotDeletePersonsProgrammeMembershipsBeforeSyncingProgramme() {
    ProgrammeMembership programmeMembership = new ProgrammeMembership();
    programmeMembership.setData(new HashMap<>(Map.of(
        DATA_PERSON_ID, ALL_PERSON_ID,
        DATA_PROGRAMME_ID, PROGRAMME_MEMBERSHIP_A11_PROGRAMME_ID,
        DATA_CURRICULUM_ID, PROGRAMME_MEMBERSHIP_A11_CURRICULUM_ID,
        DATA_PROGRAMME_MEMBERSHIP_TYPE, ALL_PROGRAMME_MEMBERSHIP_TYPE,
        DATA_PROGRAMME_START_DATE, ALL_PROGRAMME_START_DATE,
        DATA_PROGRAMME_END_DATE, ALL_PROGRAMME_END_DATE,
        DATA_PROGRAMME_COMPLETION_DATE, PROGRAMME_MEMBERSHIP_A11_PROGRAMME_COMPLETION_DATE)));

    Programme programme = new Programme();
    programme.setTisId(PROGRAMME_1_ID);
    programme.setData(Map.of(
        PROGRAMME_NAME, PROGRAMME_1_NAME
    ));
    Curriculum curriculum = new Curriculum();
    curriculum.setTisId(CURRICULUM_1_ID);
    curriculum.setData(Map.of(
        CURRICULUM_NAME, CURRICULUM_1_NAME
    ));

    when(curriculumService.findById(CURRICULUM_1_ID)).thenReturn(Optional.of(curriculum));
    when(programmeMembershipService.findByProgrammeId(PROGRAMME_1_ID))
        .thenReturn(Collections.singleton(programmeMembership));
    when(programmeMembershipService.findBySimilar(ALL_PERSON_ID, PROGRAMME_1_ID,
        ALL_PROGRAMME_MEMBERSHIP_TYPE, ALL_PROGRAMME_START_DATE, ALL_PROGRAMME_END_DATE))
        .thenReturn(Collections.singleton(programmeMembership));

    enricher.enrich(programme);

    // the initial 'DELETE' and then 'LOAD' sync would both use programmeMembership
    // we only want one invocation for 'LOAD'
    verify(tcsSyncService, times(1)).syncRecord(programmeMembership);
  }

  @Test
  void shouldSkipSimilarProgrammeMembershipsWhenReloadingPersonsProgrammeMemberships() {
    ProgrammeMembership programmeMembership1 = new ProgrammeMembership();
    programmeMembership1.setData(new HashMap<>(Map.of(
        DATA_TIS_ID, PROGRAMME_MEMBERSHIP_A11_TIS_ID,
        DATA_PERSON_ID, ALL_PERSON_ID,
        DATA_PROGRAMME_ID, PROGRAMME_MEMBERSHIP_A11_PROGRAMME_ID,
        DATA_CURRICULUM_ID, PROGRAMME_MEMBERSHIP_A11_CURRICULUM_ID,
        DATA_PROGRAMME_MEMBERSHIP_TYPE, ALL_PROGRAMME_MEMBERSHIP_TYPE,
        DATA_PROGRAMME_START_DATE, ALL_PROGRAMME_START_DATE,
        DATA_PROGRAMME_END_DATE, ALL_PROGRAMME_END_DATE,
        DATA_PROGRAMME_COMPLETION_DATE, PROGRAMME_MEMBERSHIP_A11_PROGRAMME_COMPLETION_DATE)));
    programmeMembership1.setTisId(PROGRAMME_MEMBERSHIP_A11_TIS_ID);
    ProgrammeMembership programmeMembership2 = new ProgrammeMembership();
    programmeMembership2.setData(new HashMap<>(Map.of(
        DATA_TIS_ID, PROGRAMME_MEMBERSHIP_A12_TIS_ID,
        DATA_PERSON_ID, ALL_PERSON_ID,
        DATA_PROGRAMME_ID, PROGRAMME_MEMBERSHIP_A12_PROGRAMME_ID,
        DATA_CURRICULUM_ID, PROGRAMME_MEMBERSHIP_A12_CURRICULUM_ID,
        DATA_PROGRAMME_MEMBERSHIP_TYPE, ALL_PROGRAMME_MEMBERSHIP_TYPE,
        DATA_PROGRAMME_START_DATE, ALL_PROGRAMME_START_DATE,
        DATA_PROGRAMME_END_DATE, ALL_PROGRAMME_END_DATE,
        DATA_PROGRAMME_COMPLETION_DATE, PROGRAMME_MEMBERSHIP_A12_PROGRAMME_COMPLETION_DATE)));
    programmeMembership2.setTisId(PROGRAMME_MEMBERSHIP_A12_TIS_ID);

    Programme programme1 = new Programme();
    programme1.setTisId(PROGRAMME_1_ID);
    programme1.setData(Map.of(
        PROGRAMME_NAME, PROGRAMME_1_NAME
    ));
    Curriculum curriculum1 = new Curriculum();
    curriculum1.setTisId(CURRICULUM_1_ID);
    curriculum1.setData(Map.of(
        CURRICULUM_NAME, CURRICULUM_1_NAME
    ));
    Curriculum curriculum2 = new Curriculum();
    curriculum2.setTisId(CURRICULUM_2_ID);
    curriculum2.setData(Map.of(
        CURRICULUM_NAME, CURRICULUM_2_NAME
    ));

    when(curriculumService.findById(CURRICULUM_1_ID)).thenReturn(Optional.of(curriculum1));
    when(curriculumService.findById(CURRICULUM_2_ID)).thenReturn(Optional.of(curriculum2));
    when(programmeService.findById(PROGRAMME_1_ID)).thenReturn(Optional.of(programme1));
    when(programmeMembershipService.findByPersonId(ALL_PERSON_ID))
        .thenReturn(Sets.newSet(programmeMembership1, programmeMembership2));
    when(programmeMembershipService.findBySimilar(ALL_PERSON_ID, PROGRAMME_1_ID,
        ALL_PROGRAMME_MEMBERSHIP_TYPE, ALL_PROGRAMME_START_DATE, ALL_PROGRAMME_END_DATE))
        .thenReturn(Sets.newSet(programmeMembership1, programmeMembership2));

    enricher.enrich(programmeMembership1);
    verify(enricher, times(1)).syncAggregateProgrammeMembership(programmeMembership1, true);
    verify(enricher, times(0)).syncAggregateProgrammeMembership(programmeMembership2, false);

    // the initial 'DELETE' and then 'LOAD' sync both use programmeMembership
    verify(tcsSyncService, times(2)).syncRecord(programmeMembership1);
  }

  @Test
  void shouldNotSkipDisSimilarProgrammeMembershipsWhenReloadingPersonsProgrammeMemberships() {
    ProgrammeMembership programmeMembership1 = new ProgrammeMembership();
    programmeMembership1.setData(new HashMap<>(Map.of(
        DATA_TIS_ID, PROGRAMME_MEMBERSHIP_A11_TIS_ID,
        DATA_PERSON_ID, ALL_PERSON_ID,
        DATA_PROGRAMME_ID, PROGRAMME_MEMBERSHIP_A11_PROGRAMME_ID,
        DATA_CURRICULUM_ID, PROGRAMME_MEMBERSHIP_A11_CURRICULUM_ID,
        DATA_PROGRAMME_MEMBERSHIP_TYPE, ALL_PROGRAMME_MEMBERSHIP_TYPE,
        DATA_PROGRAMME_START_DATE, ALL_PROGRAMME_START_DATE,
        DATA_PROGRAMME_END_DATE, ALL_PROGRAMME_END_DATE,
        DATA_PROGRAMME_COMPLETION_DATE, PROGRAMME_MEMBERSHIP_A11_PROGRAMME_COMPLETION_DATE)));
    programmeMembership1.setTisId(PROGRAMME_MEMBERSHIP_A11_TIS_ID);
    ProgrammeMembership programmeMembership2 = new ProgrammeMembership();
    programmeMembership2.setData(new HashMap<>(Map.of(
        DATA_TIS_ID, PROGRAMME_MEMBERSHIP_A32_TIS_ID,
        DATA_PERSON_ID, ALL_PERSON_ID,
        DATA_PROGRAMME_ID, PROGRAMME_MEMBERSHIP_A32_PROGRAMME_ID,
        DATA_CURRICULUM_ID, PROGRAMME_MEMBERSHIP_A32_CURRICULUM_ID,
        DATA_PROGRAMME_MEMBERSHIP_TYPE, ALL_PROGRAMME_MEMBERSHIP_TYPE,
        DATA_PROGRAMME_START_DATE, ALL_PROGRAMME_START_DATE,
        DATA_PROGRAMME_END_DATE, ALL_PROGRAMME_END_DATE,
        DATA_PROGRAMME_COMPLETION_DATE, PROGRAMME_MEMBERSHIP_A32_PROGRAMME_COMPLETION_DATE)));
    programmeMembership2.setTisId(PROGRAMME_MEMBERSHIP_A32_TIS_ID);

    Programme programme1 = new Programme();
    programme1.setTisId(PROGRAMME_1_ID);
    programme1.setData(Map.of(
        PROGRAMME_NAME, PROGRAMME_1_NAME
    ));
    Programme programme3 = new Programme();
    programme3.setTisId(PROGRAMME_3_ID);
    programme3.setData(Map.of(
        PROGRAMME_NAME, PROGRAMME_3_NAME
    ));
    Curriculum curriculum1 = new Curriculum();
    curriculum1.setTisId(CURRICULUM_1_ID);
    curriculum1.setData(Map.of(
        CURRICULUM_NAME, CURRICULUM_1_NAME
    ));
    Curriculum curriculum2 = new Curriculum();
    curriculum2.setTisId(CURRICULUM_2_ID);
    curriculum2.setData(Map.of(
        CURRICULUM_NAME, CURRICULUM_2_NAME
    ));

    when(curriculumService.findById(CURRICULUM_1_ID)).thenReturn(Optional.of(curriculum1));
    when(curriculumService.findById(CURRICULUM_2_ID)).thenReturn(Optional.of(curriculum2));
    when(programmeService.findById(PROGRAMME_1_ID)).thenReturn(Optional.of(programme1));
    when(programmeService.findById(PROGRAMME_3_ID)).thenReturn(Optional.of(programme3));
    when(programmeMembershipService.findByPersonId(ALL_PERSON_ID))
        .thenReturn(Sets.newSet(programmeMembership1, programmeMembership2));
    when(programmeMembershipService.findBySimilar(ALL_PERSON_ID, PROGRAMME_1_ID,
        ALL_PROGRAMME_MEMBERSHIP_TYPE, ALL_PROGRAMME_START_DATE, ALL_PROGRAMME_END_DATE))
        .thenReturn(Sets.newSet(programmeMembership1));
    when(programmeMembershipService.findBySimilar(ALL_PERSON_ID, PROGRAMME_3_ID,
        ALL_PROGRAMME_MEMBERSHIP_TYPE, ALL_PROGRAMME_START_DATE, ALL_PROGRAMME_END_DATE))
        .thenReturn(Sets.newSet(programmeMembership2));

    enricher.enrich(programmeMembership1);
    verify(enricher, times(1)).syncAggregateProgrammeMembership(programmeMembership1, true);
    verify(enricher, times(1)).syncAggregateProgrammeMembership(programmeMembership2, false);

    // the initial 'DELETE' and then 'LOAD' sync both use programmeMembership
    verify(tcsSyncService, times(2)).syncRecord(programmeMembership1);
  }

  @Test
  void shouldDeleteSolitaryProgrammeMembership() {
    ProgrammeMembership programmeMembership = new ProgrammeMembership();
    programmeMembership.setData(new HashMap<>(Map.of(
        DATA_TIS_ID, PROGRAMME_MEMBERSHIP_A11_TIS_ID,
        DATA_PERSON_ID, ALL_PERSON_ID)));
    programmeMembership.setTisId(PROGRAMME_MEMBERSHIP_A11_TIS_ID);

    when(programmeMembershipService.findByPersonId(ALL_PERSON_ID))
        .thenReturn(Collections.emptySet());


    enricher.programmeMembershipBeforeDelete(PROGRAMME_MEMBERSHIP_A11_TIS_ID);
    enricher.programmeMembershipAfterDelete(PROGRAMME_MEMBERSHIP_A11_TIS_ID);

    verify(tcsSyncService).syncRecord(programmeMembership);
  }

  @Test
  void shouldDeleteProgrammeMembershipFromSet() {
    ProgrammeMembership programmeMembership = new ProgrammeMembership();
    programmeMembership.setData(new HashMap<>(Map.of(
        DATA_TIS_ID, PROGRAMME_MEMBERSHIP_A11_TIS_ID,
        DATA_PERSON_ID, ALL_PERSON_ID)));
    programmeMembership.setTisId(PROGRAMME_MEMBERSHIP_A11_TIS_ID);

    ProgrammeMembership programmeMembership1 = new ProgrammeMembership();
    programmeMembership1.setData(new HashMap<>(Map.of(
        DATA_TIS_ID, PROGRAMME_MEMBERSHIP_A32_TIS_ID,
        DATA_PERSON_ID, ALL_PERSON_ID,
        DATA_PROGRAMME_ID, PROGRAMME_MEMBERSHIP_A32_PROGRAMME_ID,
        DATA_CURRICULUM_ID, PROGRAMME_MEMBERSHIP_A32_CURRICULUM_ID,
        DATA_PROGRAMME_MEMBERSHIP_TYPE, ALL_PROGRAMME_MEMBERSHIP_TYPE,
        DATA_PROGRAMME_START_DATE, ALL_PROGRAMME_START_DATE,
        DATA_PROGRAMME_END_DATE, ALL_PROGRAMME_END_DATE,
        DATA_PROGRAMME_COMPLETION_DATE, PROGRAMME_MEMBERSHIP_A32_PROGRAMME_COMPLETION_DATE)));
    programmeMembership1.setTisId(PROGRAMME_MEMBERSHIP_A32_TIS_ID);

    Programme programme3 = new Programme();
    programme3.setTisId(PROGRAMME_3_ID);
    programme3.setData(Map.of(
        PROGRAMME_NAME, PROGRAMME_3_NAME
    ));
    Curriculum curriculum2 = new Curriculum();
    curriculum2.setTisId(CURRICULUM_2_ID);
    curriculum2.setData(Map.of(
        CURRICULUM_NAME, CURRICULUM_2_NAME
    ));

    when(curriculumService.findById(CURRICULUM_2_ID)).thenReturn(Optional.of(curriculum2));
    when(programmeService.findById(PROGRAMME_3_ID)).thenReturn(Optional.of(programme3));
    when(programmeMembershipService.findByPersonId(ALL_PERSON_ID))
        .thenReturn(Collections.singleton(programmeMembership1));
    when(programmeMembershipService
        .findBySimilar(ALL_PERSON_ID, PROGRAMME_MEMBERSHIP_A32_PROGRAMME_ID,
        ALL_PROGRAMME_MEMBERSHIP_TYPE, ALL_PROGRAMME_START_DATE, ALL_PROGRAMME_END_DATE))
        .thenReturn(Collections.singleton(programmeMembership1));

    enricher.programmeMembershipBeforeDelete(PROGRAMME_MEMBERSHIP_A11_TIS_ID);
    enricher.programmeMembershipAfterDelete(PROGRAMME_MEMBERSHIP_A11_TIS_ID);

    verify(enricher, times(1))
        .syncAggregateProgrammeMembership(programmeMembership1, false);

    // 1 delete + 1 load
    verify(tcsSyncService, times(1)).syncRecord(programmeMembership);
    verify(tcsSyncService, times(1)).syncRecord(programmeMembership1);
  }

  /**
   * Get the Curricula from the curricula JSON string.
   *
   * @param curriculaJson The JSON string to get the curricula from.
   * @return The curricula.
   */
  private Set<Map<String,String>> getCurriculaFromJson(String curriculaJson) {
    ObjectMapper mapper = new ObjectMapper();

    Set<Map<String, String>> curricula = new HashSet<>();
    try {
      curricula = mapper.readValue(curriculaJson, new TypeReference<Set<Map<String, String>>>() {
      });
    } catch (Exception e) {
      e.printStackTrace();
    }

    return curricula;
  }
}
