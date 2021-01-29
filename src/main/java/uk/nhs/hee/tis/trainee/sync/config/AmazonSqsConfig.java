package uk.nhs.hee.tis.trainee.sync.config;

import com.amazonaws.ClientConfiguration;
import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.AmazonSQSClientBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AmazonSqsConfig {

  @Value("${application.aws.access.key.id}")
  private String awsAccessKeyId;

  @Value("${application.aws.secret.access.key}")
  private String awsSecretAccessKey;

  @Value("${application.aws.region}")
  private String region;

  AWSCredentialsProvider credentialsProvider() {
    BasicAWSCredentials basic = new BasicAWSCredentials(this.awsAccessKeyId,
        this.awsSecretAccessKey);
    return new AWSStaticCredentialsProvider(basic);
  }

  @Bean
  public AmazonSQS amazonSqs() {
    AmazonSQSClientBuilder amazonSQSClientBuilder = AmazonSQSClientBuilder.standard();
    amazonSQSClientBuilder.setRegion(region);
    amazonSQSClientBuilder.setCredentials(credentialsProvider());
    amazonSQSClientBuilder.setClientConfiguration(new ClientConfiguration());
    return amazonSQSClientBuilder.build();
  }

}
