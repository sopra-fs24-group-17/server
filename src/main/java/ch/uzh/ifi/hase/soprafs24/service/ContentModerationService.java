package ch.uzh.ifi.hase.soprafs24.service;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.*;
import java.util.Collections;

@Service
public class ContentModerationService {

    private Gson gson = new Gson();

    private final String apiKey = "AIzaSyChH64OxzTiy4iWJBxGyMW5GG-gyBksGU8";

    private final String apiUrl = "https://commentanalyzer.googleapis.com/v1alpha1/comments:analyze?key=" + apiKey;

    Logger logger = LoggerFactory.getLogger(ContentModerationService.class);

    public double checkToxicity(String comment) {
        String requestBody = String.format("{\"comment\": {\"text\": \"%s\"}, \"requestedAttributes\": {\"TOXICITY\": {}}}", comment);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));

        HttpEntity<String> entity = new HttpEntity<>(requestBody, headers);
        RestTemplate restTemplate = new RestTemplate();

        try {
            ResponseEntity<String> response = restTemplate.exchange(apiUrl, HttpMethod.POST, entity, String.class);
            return parseSummaryScore(response.getBody());
        } catch (HttpClientErrorException e) {
            JsonObject errorResponse = gson.fromJson(e.getResponseBodyAsString(), JsonObject.class);
            String errorCode = errorResponse.getAsJsonObject("error").get("code").getAsString();
            if (errorCode.equals("400") && errorResponse.getAsJsonObject("error").get("message").getAsString().contains("LANGUAGE_NOT_SUPPORTED_BY_ATTRIBUTE")) {
                logger.error("Unsupported language for TOXICITY attribute.");
                return 0.0;
            }
            logger.error("Error requesting toxicity check: " + e.getStatusCode() + " " + e.getResponseBodyAsString());
            return 0.0;
        } catch (Exception e) {
            logger.error("General error in toxicity check: ", e);
            return 0.0;
        }
    }

    public double parseSummaryScore(String jsonResponse) {
        try {
            JsonObject rootObj = gson.fromJson(jsonResponse, JsonObject.class);
            JsonObject attributeScores = rootObj.getAsJsonObject("attributeScores");
            JsonObject toxicity = attributeScores.getAsJsonObject("TOXICITY");
            JsonObject summaryScore = toxicity.getAsJsonObject("summaryScore");
            double value = summaryScore.get("value").getAsDouble();
            logger.info(String.valueOf(value));
            return value;
        } catch (Exception e) {
            logger.error("Failed to parse the summary score from response", e);
            return -1;
        }
    }
}
