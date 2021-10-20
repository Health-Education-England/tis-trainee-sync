package uk.nhs.hee.tis.trainee.sync.config;

import com.amazonaws.services.sqs.AmazonSQSAsync;
import com.amazonaws.services.sqs.AmazonSQSAsyncClientBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Profile;

@TestConfiguration
public class AmazonSqsConfigTest {

  @Value("${application.aws.region}")
  private String region;

  /**
   * Create a default {@link AmazonSQSAsync} bean.
   *
   * @return The created bean.
   */
  @Bean
  @Profile("test")
  public AmazonSQSAsync amazonSqsAsync() {
    AmazonSQSAsyncClientBuilder builder = AmazonSQSAsyncClientBuilder.standard();
    builder.setRegion(region);
    return builder.build();
  }
}
