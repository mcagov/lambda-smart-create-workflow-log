package uk.mcga.smart.lambda;

import static org.apache.commons.lang3.StringUtils.contains;
import static org.apache.commons.lang3.StringUtils.substringBetween;

import java.util.function.Function;

import org.springframework.cloud.function.json.JsonMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.Message;

import com.fasterxml.jackson.databind.exc.InvalidFormatException;

import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Validator;
import lombok.RequiredArgsConstructor;
import lombok.val;
import lombok.extern.slf4j.Slf4j;
import uk.mcga.smart.lambda.config.Config;
import uk.mcga.smart.lambda.exception.SmartException;
import uk.mcga.smart.lambda.exception.TokenException;
import uk.mcga.smart.lambda.model.WorkflowLogRequest;
import uk.mcga.smart.lambda.model.WorkflowLogResponse;
import uk.mcga.smart.lambda.service.WorkflowLogService;

@Configuration
@RequiredArgsConstructor
@Slf4j
public class Handler {

    @Bean
    Function<Message<String>, WorkflowLogResponse> WorkflowLog(
        Validator validator,
        WorkflowLogService service,
        Config config,
        JsonMapper mapper){
        return req -> {
            WorkflowLogResponse res;
            try {
                String value = req.getPayload();
                log.info("Received WorkflowLogRequest: {}", value);
                WorkflowLogRequest workflowLogRequest = mapper.fromJson(value, WorkflowLogRequest.class);
                var violations = validator.validate(workflowLogRequest);
                if(!violations.isEmpty() ) {
                    throw new ConstraintViolationException(violations);
                }

                service.generateWorkflowLog(workflowLogRequest);
                res = WorkflowLogResponse
                    .builder()
                    .status(200)
                    .build();

                // TODO figure out a central way of handling exceptions
            } catch (ConstraintViolationException e) {
                log.error("ConstraintViolationException: {}", e.getMessage(), e);
                res = WorkflowLogResponse
                    .builder()
                    .status(400)
                    .error(e.getMessage())
                    .build();
            } catch (IllegalStateException e) {
                log.error("IllegalStateException: {}", e.getMessage(), e);
                if(e.getCause() instanceof InvalidFormatException ife ) {
                    val location = substringBetween(ife.getPathReference(), "\"");
                    log.debug("location {}", location);

                    if(contains(ife.getMessage(), "Cannot deserialize value of type `java.util.UUID`") ) {
                        val msg = new StringBuilder(location);
                        msg.append(": The value '");
                        msg.append(ife.getValue());
                        msg.append("' is not a valid UUID");
                        res = WorkflowLogResponse
                            .builder()
                            .status(400)
                            .error(msg.toString())
                            .build();

                    } else {
                        val msg = new StringBuilder(location);
                        msg.append(": The value '");
                        msg.append(ife.getValue());
                        msg.append("' is not a valid");
                        res = WorkflowLogResponse
                            .builder()
                            .status(400)
                            .error(msg.toString())
                            .build();
                    }
                } else {
                    res = WorkflowLogResponse
                        .builder()
                        .status(500)
                        .error("Unknown Exception - " + e.getMessage())
                        .build();
                }
            } catch (SmartException e) {
                log.error("SmartException: {}", e.getMessage(), e);
                res = WorkflowLogResponse
                    .builder()
                    .status(e.getStatusCode())
                    .error(e.getMessage())
                    .build();
            } catch (TokenException e) {
                log.error("TokenException: {}", e.getMessage(), e);
                res = WorkflowLogResponse
                    .builder()
                    .status(401)
                    .error(e.getMessage())
                    .build();
            } catch (Throwable e) {
                log.error("Exception: {}", e.getMessage(), e);
                res = WorkflowLogResponse
                    .builder()
                    .status(500)
                    .error("Unknown Exception - " + e.getMessage())
                    .build();
            }
            log.info("Returning: {}", res);
            return res;
        };
    }

}
