package uk.nhs.hee.tis.trainee.sync.api;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import uk.nhs.hee.tis.trainee.sync.dto.RecordDto;

@ExtendWith(SpringExtension.class)
@WebMvcTest(controllers = RecordResource.class)
public class RecordResourceTest {

  private static final String DEFFAULT_RECORD_ID = "DEFFAULT_RECORD_ID";
  private static final String DEFFAULT_SCHEMA_NAME = "DEFFAULT_SCHEMA_NAME";

  private MockMvc mockMvc;

  @Autowired
  private MappingJackson2HttpMessageConverter jacksonMessageConverter;

  private RecordDto recordDto;

  /**
   * Set up mocks before each test.
   */
  @BeforeEach
  public void setup() {
    RecordResource gradeResource = new RecordResource();
    mockMvc = MockMvcBuilders.standaloneSetup(gradeResource)
        .setMessageConverters(jacksonMessageConverter)
        .build();
  }

  @BeforeEach
  public void initData() {
    recordDto = new RecordDto();
    Map<String, String> data = new HashMap<>();
    data.put("id", DEFFAULT_RECORD_ID);
    recordDto.setData(data);
    Map<String, String> metadata = new HashMap<>();
    metadata.put("table-name", DEFFAULT_SCHEMA_NAME);
    recordDto.setMetadata(metadata);
  }

  @Test
  void testPostARecord() throws Exception {
    this.mockMvc.perform(post("/api/record")
        .contentType(TestUtil.APPLICATION_JSON_UTF8)
        .content(TestUtil.convertObjectToJsonBytes(recordDto)))
        .andExpect(status().isOk());
  }
}
