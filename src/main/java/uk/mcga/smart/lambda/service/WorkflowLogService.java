package uk.mcga.smart.lambda.service;

import static org.apache.commons.lang3.StringUtils.isBlank;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.retry.support.RetrySynchronizationManager;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;
import org.springframework.util.StreamUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;
import lombok.val;
import lombok.extern.slf4j.Slf4j;
import uk.mcga.smart.lambda.exception.SmartException;
import uk.mcga.smart.lambda.model.WorkflowLogRequest;
import uk.mcga.smart.lambda.okta.OktaService;

@Service
@RequiredArgsConstructor
@Slf4j
public class WorkflowLogService {

    private static final String WORKFLOWLOGS_SCOPE = "workflowlogs:write:all";
    private final RestClient restClient;
    private final OktaService oktaService;

    @Value("${smart.forecast.smart-api-uri}")
    private String smartApiUri;

    @Retryable(maxAttempts = 3, backoff = @Backoff(delay = 2000), include = RuntimeException.class)
    public void generateWorkflowLog(WorkflowLogRequest workflowLogRequest) throws Exception{
        Assert.hasText(smartApiUri, "null or empty smartApiUri");

        log.debug("workflowLogRequest: {}", workflowLogRequest);
        if(RetrySynchronizationManager.getContext().getRetryCount() > 0 ) {
            log.warn("workflowLogRequest: Retrying request {}", RetrySynchronizationManager.getContext());
        }

        val accessToken = oktaService.getToken(WORKFLOWLOGS_SCOPE);

        val workflowLogUrl = UriComponentsBuilder
            .fromUriString(smartApiUri)
            .path("/v1/workflow_log")
            .build().toUri();

        log.debug("workflowLogUrl: {}", workflowLogUrl);

        val json = restClient
            .post()
            .uri(workflowLogUrl)
            .headers(headers -> {
                headers.setBearerAuth(accessToken);
            })
            .body(workflowLogRequest)
            .retrieve()
            .onStatus(HttpStatusCode::is4xxClientError, (request, response) -> {
                val message = tryGetMessage(response);
                log.error("response: {}", message);
                if(response.getStatusCode() == HttpStatus.FORBIDDEN ||
                    response.getStatusCode() == HttpStatus.UNAUTHORIZED ) {
                    // evict the token
                    oktaService.evictToken(WORKFLOWLOGS_SCOPE);
                }
                throw new SmartException("Failed to call WorkflowLog api, response " +
                    message, response.getStatusCode().value());
            })
            .onStatus(HttpStatusCode::is5xxServerError, (request, response) -> {
                val message = tryGetMessage(response);
                log.error("response: {}", message);
                throw new SmartException("Failed to call WorkflowLog api, response " +
                    message, response.getStatusCode().value());
            })
            .body(JsonNode.class);

        log.debug("json: {}", json);
    }

    private String tryGetMessage(ClientHttpResponse response) throws IOException{
        val resStr = StreamUtils.copyToString(response.getBody(), StandardCharsets.UTF_8);
        try {
            log.debug("tryGetMessage: !isBlank(resStr) " + !isBlank(resStr));
            if(!isBlank(resStr) ) {
                ObjectMapper mapper = new ObjectMapper();
                val resJson = mapper.readValue(resStr, JsonNode.class);
                log.debug("tryGetMessage: resJson " + resJson);
                if(resJson != null && resJson.has("message") ) {
                    return resJson.get("message").asText();
                } else {
                    log.warn("tryGetMessage: message not found in  " + resJson);
                }
            } else {
                log.warn("tryGetMessage: response body is blank");
            }

        } catch (Exception e) {
            log.error("tryGetMessage: Exception ", e);
        }

        return resStr == null ? "" : resStr;

    }
}
