/*
 * The MIT License (MIT)
 *
 * Copyright 2020 Crown Copyright (Health Education England)
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

package uk.nhs.hee.tis.trainee.sync.mapper.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import org.mapstruct.Qualifier;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class TraineeDetailsUtil {

  @Qualifier
  @Target(ElementType.METHOD)
  @Retention(RetentionPolicy.SOURCE)
  public @interface Curricula {

  }

  @Qualifier
  @Target(ElementType.METHOD)
  @Retention(RetentionPolicy.SOURCE)
  public @interface ConditionsOfJoining {

  }

  @Qualifier
  @Target(ElementType.METHOD)
  @Retention(RetentionPolicy.SOURCE)
  public @interface ResponsibleOfficer {

  }

  @Qualifier
  @Target(ElementType.METHOD)
  @Retention(RetentionPolicy.SOURCE)
  public @interface OtherSites {

  }

  @Qualifier
  @Target(ElementType.METHOD)
  @Retention(RetentionPolicy.SOURCE)
  public @interface OtherSpecialties {

  }

  @Qualifier
  @Target(ElementType.METHOD)
  @Retention(RetentionPolicy.SOURCE)
  public @interface WholeTimeEquivalent {

  }

  /**
   * Gets the set of curricula from the data map String.
   *
   * @param data the data containing the curricula as a string
   * @return the curricula
   */
  @Curricula
  public Set<Map<String, String>> curricula(Map<String, String> data) {
    ObjectMapper mapper = new ObjectMapper();

    Set<Map<String, String>> curricula = new HashSet<>();
    if (data.get("curricula") != null) {
      try {
        curricula = mapper.readValue(data.get("curricula"), new TypeReference<>() {
        });
      } catch (JsonProcessingException e) {
        log.error("Badly formed curricula JSON in {}", data);
      }
    }

    return curricula;
  }

  /**
   * Gets the Conditions of joining from the data map String.
   *
   * @param data the data containing the conditions of joining as a string
   * @return the conditions of joining
   */
  @ConditionsOfJoining
  public Map<String, String> conditionsOfJoining(Map<String, String> data) {
    ObjectMapper mapper = new ObjectMapper();

    Map<String, String> conditionsOfJoining = new HashMap<>();
    if (data.get("conditionsOfJoining") != null) {
      try {
        conditionsOfJoining = mapper.readValue(data.get("conditionsOfJoining"),
            new TypeReference<>() {
            });
      } catch (JsonProcessingException e) {
        log.error("Badly formed Conditions of joining JSON in {}", data);
      }
    }

    return conditionsOfJoining;
  }

  /**
   * Gets the Responsible officer from the data map String.
   *
   * @param data the data containing the responsible officer as a string
   * @return the responsible officer
   */
  @ResponsibleOfficer
  public Map<String, String> responsibleOfficer(Map<String, String> data) {
    ObjectMapper mapper = new ObjectMapper();

    Map<String, String> responsibleOfficer = new HashMap<>();
    if (data.get("responsibleOfficer") != null) {
      try {
        responsibleOfficer = mapper.readValue(data.get("responsibleOfficer"),
            new TypeReference<>() {
            });
      } catch (JsonProcessingException e) {
        log.error("Badly formed Responsible officer JSON in {}", data);
      }
    }

    return responsibleOfficer;
  }

  /**
   * Gets the set of other sites from the data map String.
   *
   * @param data the data containing the other sites as a string
   * @return the other sites
   */
  @OtherSites
  public Set<Map<String, String>> otherSites(Map<String, String> data) {
    ObjectMapper mapper = new ObjectMapper();

    Set<Map<String, String>> otherSites = new HashSet<>();
    if (data.get("otherSites") != null) {
      try {
        otherSites = mapper.readValue(data.get("otherSites"), new TypeReference<>() {
        });
      } catch (JsonProcessingException e) {
        log.error("Badly formed other sites JSON in {}", data);
      }
    }

    return otherSites;
  }

  /**
   * Gets the set of other specialties from the data map String.
   *
   * @param data the data containing the other specialties as a string
   * @return the other specialties
   */
  @OtherSpecialties
  public Set<Map<String, String>> otherSpecialties(Map<String, String> data) {
    ObjectMapper mapper = new ObjectMapper();

    Set<Map<String, String>> otherSpecialties = new HashSet<>();
    if (data.get("otherSpecialties") != null) {
      try {
        otherSpecialties = mapper.readValue(data.get("otherSpecialties"), new TypeReference<>() {
        });
      } catch (JsonProcessingException e) {
        log.error("Badly formed other specialties JSON in {}", data);
      }
    }

    return otherSpecialties;
  }

  /**
   * Gets the whole time equivalent from one of two possible field names.
   *
   * @param data the data containing the whole time equivalent.
   * @return The whole time equivalent value.
   */
  @WholeTimeEquivalent
  public String wholeTimeEquivalent(Map<String, String> data) {
    // TODO: remove once requested Placement data from TCS is standardised.
    return data.getOrDefault("placementWholeTimeEquivalent", data.get("wholeTimeEquivalent"));
  }
}
