/*
 * The MIT License (MIT)
 *
 *  Copyright 2023 Crown Copyright (Health Education England)
 *
 *  Permission is hereby granted, free of charge, to any person obtaining a copy of this software
 *  and associated documentation files (the "Software"), to deal in the Software without
 *  restriction, including without limitation the rights to use, copy, modify, merge, publish,
 *  distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the
 *  Software is furnished to do so, subject to the following conditions:
 *
 *  The above copyright notice and this permission notice shall be included in all copies or
 *  substantial portions of the Software.
 *
 *  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING
 *  BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 *  NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM,
 *  DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 *  OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package uk.nhs.hee.tis.trainee.sync.repository;

import java.util.Set;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;
import uk.nhs.hee.tis.trainee.sync.model.PostSpecialty;

@Repository
public interface PostSpecialtyRepository extends MongoRepository<PostSpecialty, String> {

  /**
   * Find PostSpecialties of type SUB_SPECIALTY for the given post.
   *
   * @param postId The post ID to filter by.
   * @return The found PostSpecialties, empty if no results.
   */
  @Query("{ $and: [ {'data.postId' : ?0}, { 'data.postSpecialtyType' : \"SUB_SPECIALTY\"} ] }")
  Set<PostSpecialty> findSubSpecialtiesByPostId(String postId);

  /**
   * Find PostSpecialties of type SUB_SPECIALTY for the given specialty.
   *
   * @param specialtyId The specialty ID to filter by.
   * @return The found PostSpecialties, empty if no results.
   */
  @Query("{ $and: [ {'data.specialtyId' : ?0}, { 'data.postSpecialtyType' : \"SUB_SPECIALTY\"} ] }")
  Set<PostSpecialty> findSubSpecialtiesBySpecialtyId(String specialtyId);
}
