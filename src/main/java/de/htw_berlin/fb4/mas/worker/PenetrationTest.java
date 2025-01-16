package de.htw_berlin.fb4.mas.worker;

import org.camunda.bpm.client.task.ExternalTask;
import org.camunda.bpm.client.task.ExternalTaskHandler;
import org.camunda.bpm.client.task.ExternalTaskService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Scanner;
import java.util.Map;

/**
 * Implementierung eines {@link ExternalTaskHandler} zum Durchführen eines Penetrationstests.
 * <p>
 * Der Handler führt einen einfachen HTTP-Request zu einer öffentlich zugänglichen API aus
 * und speichert das Ergebnis als Prozessvariable.
 * <p>
 * Erwartete Konfiguration im Camunda-Modeler:
 * <ul>
 *     <li>External Task mit Topic "penetration_test"</li>
 *     <li>Prozessvariable "testUrl" (optional, Standardwert: "https://httpbin.org/get")</li>
 * </ul>
 */
public class PenetrationTest implements ExternalTaskHandler {

    private static final Logger log = LoggerFactory.getLogger(PenetrationTest.class);

    @Override
    public void execute(ExternalTask externalTask, ExternalTaskService externalTaskService) {
        log.info("Handling external task (Task ID: {} - Process Instance ID: {})", externalTask.getId(), externalTask.getProcessInstanceId());

        // URL für den Test laden
        String testUrl = externalTask.getVariable("testUrl");
        if (testUrl == null || testUrl.isEmpty()) {
            testUrl = "https://httpbin.org/get";
        }

        try {
            log.info("Starting penetration test for URL: {}", testUrl);

            // Penetrationstest durchführen
            String testResult = performPenetrationTest(testUrl);
            log.info("Penetration test completed successfully. Result: {}", testResult);

            // Ergebnis als Prozessvariable speichern
            Map<String, Object> variables = Map.of(
                    "testResult", testResult,   // Speichert das Testergebnis
                    "testUrl", testUrl         // Optional: die verwendete URL
            );

            // External Task abschließen und Variablen an den Prozess übergeben
            externalTaskService.complete(externalTask, variables);
        } catch (IOException e) {
            handleFailure(externalTask, externalTaskService, e);
        }
    }


    private String performPenetrationTest(String testUrl) throws IOException {
        // HTTP-Verbindung öffnen
        HttpURLConnection connection = (HttpURLConnection) new URL(testUrl).openConnection();
        connection.setRequestMethod("GET");
        connection.connect();

        // HTTP-Status prüfen
        int responseCode = connection.getResponseCode();
        if (responseCode != 200) {
            throw new IOException("HTTP request failed with response code: " + responseCode);
        }

        // Antwort auslesen
        try (Scanner scanner = new Scanner(connection.getInputStream())) {
            StringBuilder response = new StringBuilder();
            while (scanner.hasNext()) {
                response.append(scanner.nextLine());
            }
            return response.toString();
        }
    }

    private static void handleFailure(ExternalTask externalTask, ExternalTaskService externalTaskService, Exception exception) {
        String errorMessage = "Penetration Test Failed";

        StringWriter stackTraceWriter = new StringWriter();
        exception.printStackTrace(new PrintWriter(stackTraceWriter));
        String errorDetails = stackTraceWriter.toString();

        createIncident(externalTask, externalTaskService, errorMessage, errorDetails);
    }

    private static void createIncident(ExternalTask externalTask, ExternalTaskService externalTaskService, String errorMessage, String errorDetails) {
        int retries = 0;
        long retryTimeout = 0;

        log.error("Creating incident for process instance {} with message '{}'", externalTask.getProcessInstanceId(), errorMessage);
        externalTaskService.handleFailure(externalTask, errorMessage, errorDetails, retries, retryTimeout);
    }
}