package uk.nhs.hee.tis.trainee.sync.model;

import static org.springframework.beans.factory.config.BeanDefinition.SCOPE_PROTOTYPE;

import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

@Component(Specialty.ENTITY_NAME)
@Scope(SCOPE_PROTOTYPE)
public class Specialty extends Record {

  public static final String ENTITY_NAME = "Specialty";

}
