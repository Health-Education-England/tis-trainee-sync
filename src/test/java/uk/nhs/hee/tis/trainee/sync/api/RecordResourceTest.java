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
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.HashMap;
import java.util.Map;
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
import uk.nhs.hee.tis.trainee.sync.mapper.RecordMapper;
import uk.nhs.hee.tis.trainee.sync.mapper.util.RecordUtil;
import uk.nhs.hee.tis.trainee.sync.model.Record;
import uk.nhs.hee.tis.trainee.sync.service.RecordService;

@ContextConfiguration(classes = RecordUtil.class)
@ExtendWith(SpringExtension.class)
@WebMvcTest(controllers = RecordResource.class)
class RecordResourceTest {

  private static final String DEFAULT_RECORD_ID = "DEFAULT_RECORD_ID";
  private static final String DEFAULT_SCHEMA_NAME = "DEFAULT_SCHEMA_NAME";

  private MockMvc mockMvc;

  @MockBean
  private RecordService recordService;

  @MockBean
  private RecordMapper recordMapperMock;

  @Autowired
  private MappingJackson2HttpMessageConverter jacksonMessageConverter;

  private RecordDto recordDto;
  private Record record;

  /**
   * Set up mocks before each test.
   */
  @BeforeEach
  void setup() {
    RecordResource gradeResource = new RecordResource(recordService, recordMapperMock);
    mockMvc = MockMvcBuilders.standaloneSetup(gradeResource)
        .setMessageConverters(jacksonMessageConverter)
        .build();
  }

  @BeforeEach
  void initData() {
    recordDto = new RecordDto();
    Map<String, String> data = new HashMap<>();
    data.put("id", DEFAULT_RECORD_ID);
    recordDto.setData(data);
    Map<String, String> metadata = new HashMap<>();
    metadata.put("table-name", DEFAULT_SCHEMA_NAME);
    recordDto.setMetadata(metadata);

    record = new Record();
    record.setData(data);
    record.setMetadata(metadata);

    when(recordMapperMock.toEntity(recordDto)).thenReturn(record);
  }

  @Test
  void testPostARecord() throws Exception {
    this.mockMvc.perform(post("/api/record")
        .contentType(TestUtil.APPLICATION_JSON_UTF8)
        .content(TestUtil.convertObjectToJsonBytes(recordDto)))
        .andExpect(status().isOk());

    verify(recordService).processRecord(record);
  }
}
