package uk.mcga.smart.lambda.config;

import org.springframework.aot.hint.annotation.RegisterReflectionForBinding;
import org.springframework.context.annotation.Configuration;
import org.springframework.retry.annotation.EnableRetry;

import uk.mcga.smart.lambda.model.WorkflowEnum;
import uk.mcga.smart.lambda.model.WorkflowLogRequest;
import uk.mcga.smart.lambda.model.WorkflowLogResponse;

@Configuration
@EnableRetry
@RegisterReflectionForBinding({ WorkflowLogRequest.class, WorkflowLogResponse.class, WorkflowEnum.class })
public class Config {

}
