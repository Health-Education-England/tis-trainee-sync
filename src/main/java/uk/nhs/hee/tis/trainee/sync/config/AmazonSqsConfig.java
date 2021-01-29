package uk.nhs.hee.tis.trainee.sync.config;

import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.AmazonSQSClientBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AmazonSqsConfig {

  @Bean
  public AmazonSQS amazonSqs() {
    return AmazonSQSClientBuilder.defaultClient();
  }

}
