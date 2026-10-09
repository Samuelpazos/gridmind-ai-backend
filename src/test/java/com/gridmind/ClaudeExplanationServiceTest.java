package com.gridmind;

import com.gridmind.ai.ClaudeExplanationService;
import com.gridmind.ai.ClaudeNotConfiguredException;
import com.gridmind.domain.GridStatus;
import com.gridmind.domain.RiskLevel;
import com.gridmind.dto.AlertDto;
import com.gridmind.entity.TransformerEntity;
import com.gridmind.strategy.EvaluationResult;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class ClaudeExplanationServiceTest {

    @Test
    void sendsAlertContextToClaudeAndReturnsExplanation() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        ClaudeExplanationService service = new ClaudeExplanationService(builder, "test-api-key", "claude-test-model");

        server.expect(requestTo("https://api.anthropic.com/v1/messages"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("x-api-key", "test-api-key"))
                .andExpect(header("anthropic-version", "2023-06-01"))
                .andExpect(jsonPath("$.model").value("claude-test-model"))
                .andExpect(jsonPath("$.messages[0].content").value(org.hamcrest.Matchers.containsString("T-REF")))
                .andRespond(withSuccess("""
                        {"content":[{"type":"text","text":"La temperatura supera el umbral indicado. Verifica las lecturas del sensor siguiendo el procedimiento operativo."}],"stop_reason":"end_turn"}
                        """, MediaType.APPLICATION_JSON));

        String explanation = service.explain(referenceAlert());

        assertEquals("La temperatura supera el umbral indicado. Verifica las lecturas del sensor siguiendo el procedimiento operativo.", explanation);
        server.verify();
    }

    @Test
    void doesNotCallClaudeWhenApiKeyIsMissing() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        ClaudeExplanationService service = new ClaudeExplanationService(builder, "", "claude-test-model");

        assertFalse(service.isConfigured());
        assertThrows(ClaudeNotConfiguredException.class, () -> service.explain(referenceAlert()));
        server.verify();
    }

    @Test
    void doesNotCallClaudeForPredictionWhenApiKeyIsMissing() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        ClaudeExplanationService service = new ClaudeExplanationService(builder, "", "claude-test-model");

        assertThrows(ClaudeNotConfiguredException.class, () -> service.predictFailure(
                TransformerEntity.builder().code("T-REF").build(),
                Map.of("TEMPERATURE", 70.0)
        ));
        server.verify();
    }

    @Test
    void predictsRiskFromCurrentSensorValuesAndDerivesRiskLevel() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        ClaudeExplanationService service = new ClaudeExplanationService(builder, "test-api-key", "claude-test-model");
        Map<String, Double> readings = Map.of(
                "TEMPERATURE", 87.0,
                "VOLTAGE", 13.8,
                "CURRENT", 420.0,
                "VIBRATION", 5.8
        );

        server.expect(requestTo("https://api.anthropic.com/v1/messages"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("x-api-key", "test-api-key"))
                .andExpect(header("anthropic-version", "2023-06-01"))
                .andExpect(jsonPath("$.max_tokens").value(384))
                .andExpect(jsonPath("$.messages[0].content").value(org.hamcrest.Matchers.containsString("TEMPERATURE")))
                .andRespond(withSuccess("""
                        {"content":[{"type":"text","text":"{\\"failureProbability\\":0.78,\\"topFactors\\":[\\"Temperatura alta\\",\\"Vibración elevada\\"],\\"explanation\\":\\"Las lecturas indican un riesgo elevado que requiere revisión del operador.\\"}"}],"stop_reason":"end_turn"}
                        """, MediaType.APPLICATION_JSON));

        EvaluationResult result = service.predictFailure(
                TransformerEntity.builder().code("T-REF").ageYears(14).ratingKva(2000.0).build(),
                readings
        );

        assertEquals(0.78, result.getFailureProbability(), 0.0001);
        assertEquals(RiskLevel.HIGH, result.getRiskLevel());
        assertEquals("CLAUDE_API", result.getStrategyName());
        assertFalse(result.isFallbackUsed());
        assertEquals(2, result.getTopFactors().size());
        server.verify();
    }

    @Test
    void parsesPredictionWhenClaudeWrapsJsonInMarkdownFence() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        ClaudeExplanationService service = new ClaudeExplanationService(builder, "test-api-key", "claude-test-model");
        server.expect(requestTo("https://api.anthropic.com/v1/messages"))
                .andRespond(withSuccess("""
                        {"content":[{"type":"text","text":"```json\\n{\\"failureProbability\\":0.42,\\"topFactors\\":[\\"Vibración elevada\\"],\\"explanation\\":\\"La estimación es orientativa.\\"}\\n```"}],"stop_reason":"end_turn"}
                        """, MediaType.APPLICATION_JSON));

        EvaluationResult result = service.predictFailure(
                TransformerEntity.builder().code("T-REF").build(),
                Map.of("TEMPERATURE", 70.0)
        );

        assertEquals(0.42, result.getFailureProbability(), 0.0001);
        assertEquals(RiskLevel.MEDIUM, result.getRiskLevel());
        assertEquals("Vibración elevada", result.getTopFactors().getFirst());
        server.verify();
    }

    @Test
    void rejectsClaudeProbabilitiesOutsideTheValidRange() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        ClaudeExplanationService service = new ClaudeExplanationService(builder, "test-api-key", "claude-test-model");
        server.expect(requestTo("https://api.anthropic.com/v1/messages"))
                .andRespond(withSuccess("""
                        {"content":[{"type":"text","text":"{\\"failureProbability\\":1.7,\\"topFactors\\":[\\"Factor\\"],\\"explanation\\":\\"Estimate\\"}"}],"stop_reason":"end_turn"}
                        """, MediaType.APPLICATION_JSON));

        assertThrows(com.gridmind.ai.ClaudeServiceException.class, () -> service.predictFailure(
                TransformerEntity.builder().code("T-REF").build(),
                Map.of("TEMPERATURE", 70.0)
        ));
        server.verify();
    }

    private AlertDto referenceAlert() {
        return AlertDto.builder()
                .id("alert-test")
                .severity(GridStatus.CRITICAL)
                .transformerCode("T-REF")
                .transformerName("Reference transformer")
                .substationName("North substation")
                .message("Temperature exceeded the threshold.")
                .source("SENSOR_OBSERVER")
                .build();
    }
}
