package uk.nhs.hee.tis.trainee.config;

import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.AmazonSQSClientBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AmazonSQSConfig {

  @Bean
  public AmazonSQS amazonSQS() {
    return AmazonSQSClientBuilder.defaultClient();
  }

}
