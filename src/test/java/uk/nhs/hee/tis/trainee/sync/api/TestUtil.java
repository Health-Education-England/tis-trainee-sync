package uk.nhs.hee.tis.trainee.sync.api;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.nio.charset.StandardCharsets;
import org.springframework.http.MediaType;

/**
 * Utility class for testing REST controllers.
 */
public class TestUtil {

  /**
   * MediaType for JSON UTF8.
   */
  public static final MediaType APPLICATION_JSON_UTF8 = new MediaType(
      MediaType.APPLICATION_JSON.getType(),
      MediaType.APPLICATION_JSON.getSubtype(), StandardCharsets.UTF_8);

  /**
   * Convert an object to JSON byte array.
   *
   * @param object the object to convert.
   * @return the JSON byte array.
   * @throws JsonProcessingException If the object was not valid.
   */
  public static byte[] convertObjectToJsonBytes(Object object) throws JsonProcessingException {
    ObjectMapper mapper = new ObjectMapper();
    mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);

    JavaTimeModule module = new JavaTimeModule();
    mapper.registerModule(module);

    return mapper.writeValueAsBytes(object);
  }
}
