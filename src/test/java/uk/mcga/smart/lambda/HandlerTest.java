package uk.mcga.smart.lambda;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static uk.mcga.smart.lambda.model.WorkflowEnum.Forecast;

import java.net.URI;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;

import lombok.val;
import lombok.extern.slf4j.Slf4j;
import uk.mcga.smart.lambda.model.WorkflowLogRequest;
import uk.mcga.smart.lambda.model.WorkflowLogResponse;

@SpringBootTest(classes = Application.class, webEnvironment = WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@ContextConfiguration(classes = Application.class)
@Slf4j
public class HandlerTest {

    @LocalServerPort
    private int port;

    TestRestTemplate rest = new TestRestTemplate();

    private URI uri;

    private UUID trainingProviderId = UUID.fromString("f1198f11-8122-4182-bfaa-8c4ef5512d34");

    @BeforeEach
    public void init() throws Exception{
        uri = new URI("http://localhost:" + port + "/");
    }

    @Test
    void whenRequestIsGood_thenGetResult(){
        val req = new WorkflowLogRequest(Forecast, trainingProviderId, "log message");
        var res = rest.postForObject(uri, req, WorkflowLogResponse.class);

        log.info("Got {}", res);
        assertNotNull(res);
        assertEquals(200, res.getStatus());
        assertNull(res.getError());

    }

    @Test
    void whenRequestIsGood_thenGet401(){
        val req = new WorkflowLogRequest(Forecast, trainingProviderId, "log message HTTP 401");
        var res = rest.postForObject(uri, req, WorkflowLogResponse.class);
        log.info("Got {}", res);
        assertNotNull(res);
        assertEquals(401, res.getStatus());
        assertEquals("Failed to call WorkflowLog api, response Unauthorized",
            res.getError());

    }

    @Test
    void whenRequestIsGood_thenGet403(){
        val req = new WorkflowLogRequest(Forecast, trainingProviderId, "log message HTTP 403");
        var res = rest.postForObject(uri, req, WorkflowLogResponse.class);
        log.info("Got {}", res);
        assertNotNull(res);
        assertEquals(403, res.getStatus());
        assertEquals("Failed to call WorkflowLog api, response Forbidden",
            res.getError());

    }

    @Test
    void whenRequestIsGood_thenGet500(){
        val req = new WorkflowLogRequest(Forecast, trainingProviderId, "log message HTTP 500");
        var res = rest.postForObject(uri, req, WorkflowLogResponse.class);
        log.info("Got {}", res);
        assertNotNull(res);
        assertEquals(500, res.getStatus());
        assertEquals("Failed to call WorkflowLog api, response UnknownError", res.getError());

    }

    @Test
    void whenRequestMissingTP_thenThrowError(){
        ResponseEntity<WorkflowLogResponse> result = rest.exchange(
            RequestEntity.post(uri).body("""
                {
                "workflow": "Forecast",
                "message": "log message"
                }
                """), WorkflowLogResponse.class);
        log.info("Got {}", result.getBody());
        val res = result.getBody();
        assertNotNull(res);
        assertEquals(400, res.getStatus());
        assertEquals("trainingProviderId: trainingProviderId must be supplied",
            res.getError());
    }

    @Test
    void whenRequestBadTP_thenThrowError(){
        ResponseEntity<WorkflowLogResponse> result = rest.exchange(
            RequestEntity.post(uri).body("""
                {
                "workflow": "Forecast",
                "message": "log message",
                "trainingProviderId": 12334
                }
                """), WorkflowLogResponse.class);
        log.info("Got {}", result.getBody());
        val res = result.getBody();
        assertNotNull(res);
        assertEquals(400, res.getStatus());
        assertEquals("trainingProviderId: The value '12334' is not a valid UUID",
            res.getError());
    }

    @Test
    void whenRequestWithBadWorkflowLogType_thenThrowError(){
        ResponseEntity<WorkflowLogResponse> result = rest.exchange(
            RequestEntity.post(uri).body("""
                {
                "trainingProviderId": "f1198f11-8122-4182-bfaa-8c4ef5512d34",
                "workflow": "Annually",
                "message": "log message"
                }
                """), WorkflowLogResponse.class);
        log.info("Got {}", result.getBody());
        val res = result.getBody();
        assertNotNull(res);
        assertEquals(400, res.getStatus());
        assertEquals("workflow: The value 'Annually' is not a valid",
            res.getError());
    }

    @Test
    void whenRequestWithNoWorkflowLogType_thenThrowError(){
        ResponseEntity<WorkflowLogResponse> result = rest.exchange(
            RequestEntity.post(uri).body("""
                {
                "trainingProviderId": "f1198f11-8122-4182-bfaa-8c4ef5512d34",
                "message": "log message"
                }
                """), WorkflowLogResponse.class);
        log.info("Got {}", result.getBody());
        val res = result.getBody();
        assertNotNull(res);
        assertEquals(400, res.getStatus());
        assertEquals("workflow: workflow must be supplied", res.getError());

    }

    @Test
    void whenRequestWithNoMessage_thenThrowError(){
        ResponseEntity<WorkflowLogResponse> result = rest.exchange(
            RequestEntity.post(uri).body("""
                {
                "workflow": "Forecast",
                "trainingProviderId": "f1198f11-8122-4182-bfaa-8c4ef5512d34"
                }
                """), WorkflowLogResponse.class);
        log.info("Got {}", result.getBody());
        val res = result.getBody();
        assertNotNull(res);
        assertEquals(400, res.getStatus());
        assertEquals("message: message must be supplied", res.getError());

    }

}
