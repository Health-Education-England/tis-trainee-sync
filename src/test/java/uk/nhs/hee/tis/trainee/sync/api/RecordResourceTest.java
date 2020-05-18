package uk.nhs.hee.tis.trainee.sync.api;

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
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import uk.nhs.hee.tis.trainee.sync.dto.RecordDto;
import uk.nhs.hee.tis.trainee.sync.mapper.RecordMapper;
import uk.nhs.hee.tis.trainee.sync.model.Record;

@ExtendWith(SpringExtension.class)
@WebMvcTest(controllers = RecordResource.class)
public class RecordResourceTest {

  private static final String DEFAULT_RECORD_ID = "DEFAULT_RECORD_ID";
  private static final String DEFAULT_SCHEMA_NAME = "DEFAULT_SCHEMA_NAME";

  private MockMvc mockMvc;

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
  public void setup() {
    RecordResource gradeResource = new RecordResource(recordMapperMock);
    mockMvc = MockMvcBuilders.standaloneSetup(gradeResource)
        .setMessageConverters(jacksonMessageConverter)
        .build();
  }

  @BeforeEach
  public void initData() {
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
  }
}
