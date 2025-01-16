package de.htw_berlin.fb4.mas;

import de.htw_berlin.fb4.mas.worker.PrintVariables;
import de.htw_berlin.fb4.mas.worker.RunUiPathRobot;
import de.htw_berlin.fb4.mas.worker.SendMail;
import de.htw_berlin.fb4.mas.worker.PenetrationTest;
import org.camunda.bpm.client.ExternalTaskClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;

public class ExternalTaskWorkers {

    private static final Logger log = LoggerFactory.getLogger(ExternalTaskWorkers.class);

    public static void main(String[] args) {
        ExternalTaskClient client = ExternalTaskClient.create()
                .baseUrl("http://localhost:8080/engine-rest")
                .asyncResponseTimeout(20000)
                .lockDuration(10000)
                .maxTasks(1)
                .build();

        client.subscribe("print-variables").handler(new PrintVariables()).open();
        client.subscribe("send-mail").handler(new SendMail()).open();
        client.subscribe("penetration-test").handler(new PenetrationTest()).open();
        client.subscribe("RPA-Zielgruppe")
                .handler(new RunUiPathRobot(Path.of("C:\\Users\\phili\\Documents\\ZielgruppeErmitteln.1.0.1.nupkg")))
                .open();
        log.info("ExternalTaskWorkers started");
    }
}
