package uk.mcga.smart.lambda.model;

import java.util.UUID;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class WorkflowLogRequest {

    @NotNull(message = "workflow must be supplied")
    private WorkflowEnum workflow;

    @NotNull(message = "trainingProviderId must be supplied")
    private UUID trainingProviderId;

    @NotNull(message = "message must be supplied")
    private String message;
}
