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

package uk.nhs.hee.tis.trainee.sync.api;

import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Collections;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import uk.nhs.hee.tis.trainee.sync.dto.RecordDto;
import uk.nhs.hee.tis.trainee.sync.mapper.util.RecordUtil;
import uk.nhs.hee.tis.trainee.sync.service.RecordService;

@ContextConfiguration(classes = RecordUtil.class)
@ExtendWith(SpringExtension.class)
@WebMvcTest(controllers = RecordResource.class)
class RecordResourceTest {

  private MockMvc mockMvc;

  @MockBean
  private RecordService recordService;

  @Autowired
  private MappingJackson2HttpMessageConverter jacksonMessageConverter;

  /**
   * Set up mocks before each test.
   */
  @BeforeEach
  void setup() {
    RecordResource gradeResource = new RecordResource(recordService);
    mockMvc = MockMvcBuilders.standaloneSetup(gradeResource)
        .setMessageConverters(jacksonMessageConverter)
        .build();
  }

  @Test
  void testPostARecord() throws Exception {
    RecordDto recordDto = new RecordDto();
    recordDto.setData(Collections.singletonMap("id", "1"));
    recordDto.setMetadata(Collections.singletonMap("schema-name", "schema_1"));

    this.mockMvc.perform(post("/api/record")
        .contentType(TestUtil.APPLICATION_JSON_UTF8)
        .content(TestUtil.convertObjectToJsonBytes(recordDto)))
        .andExpect(status().isOk());

    verify(recordService).processRecord(recordDto);
  }
}
