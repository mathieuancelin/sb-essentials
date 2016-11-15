package org.reactivecouchbase.examples.sbessentials;

import akka.stream.javadsl.Source;
import org.reactivecouchbase.json.Json;
import org.reactivecouchbase.sbessentials.libs.actions.Action;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import scala.concurrent.duration.FiniteDuration;

import java.util.concurrent.TimeUnit;

import static org.reactivecouchbase.sbessentials.libs.result.Results.Ok;

@ComponentScan
@SpringBootApplication
public class SSEApp {

    public static void main(String[] args) {
        SpringApplication.run(SSEApp.class, args);
    }

    @RestController
    @RequestMapping("/sse")
    public static class SSEController {

        @GetMapping("/stream")
        public Action sseStream() {
            return Action.sync(ctx ->
                Ok.stream(
                    Source.tick(
                        FiniteDuration.Zero(),
                        FiniteDuration.apply(1, TimeUnit.SECONDS),
                        ""
                    )
                    .map(l -> Json.obj().with("time", System.currentTimeMillis()).with("value", l))
                    .map(Json::stringify)
                    .map(j -> "data: " + j + "\n\n")
                ).as("text/event-stream")
            );
        }
    }
}
