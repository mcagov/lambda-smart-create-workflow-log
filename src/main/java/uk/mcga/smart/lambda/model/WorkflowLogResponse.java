package uk.mcga.smart.lambda.model;

import java.time.LocalDateTime;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class WorkflowLogResponse {

    @Builder.Default
    private final LocalDateTime timestamp = LocalDateTime.now();

    private Integer status;

    private String error;

}
