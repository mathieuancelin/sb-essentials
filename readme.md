# sb-essentials

`sb-essentials` is a small library to make Spring Boot livable and Streamable. Every HTTP actionStep is defined to return an `Action` and use `Akka Streams` under the hood.

```java
@GetMapping("/hello")
public Action text() {
    ...
}
```

## Actions

Every Spring actionStep returns a `Action` that can be composed from `ActionStep`s

```java
import org.reactivecouchbase.concurrent.Future;
import org.reactivecouchbase.sbessentials.libs.actions.ActionStep;
import org.reactivecouchbase.sbessentials.libs.actions.Action;
import org.reactivecouchbase.sbessentials.libs.result.Result;
import org.reactivecouchbase.sbessentials.libs.result.Results;
import static org.reactivecouchbase.sbessentials.libs.result.Results.*;

@RestController
@RequestMapping("/api")
public static class MyController {

    @GetMapping("/hello")
    public Action text() {
        return Action.sync(ctx ->
            Ok.text("Hello World!\n")
        );
    }
}
```

`Actions`s can easily be composed from `ActionStep`s

```java
import org.reactivecouchbase.concurrent.Future;
import org.reactivecouchbase.sbessentials.libs.actions.ActionStep;
import org.reactivecouchbase.sbessentials.libs.actions.Action;
import org.reactivecouchbase.sbessentials.libs.result.Result;
import org.reactivecouchbase.sbessentials.libs.result.Results;
import static org.reactivecouchbase.sbessentials.libs.result.Results.*;

@RestController
@RequestMapping("/api")
public static class MyController {

    // ActionStep that logs before request
    private static ActionStep LogBefore = (req, block) -> {
        Long start = System.currentTimeMillis();
        logger.info("[Log] before actionStep -> {}", req.getRequest().getRequestURI());
        return block.apply(req.setValue("start", start));
    };

    // ActionStep that logs after request
    private static ActionStep LogAfter = (req, block) -> block.apply(req).andThen(ttry -> {
        logger.info(
            "[Log] after actionStep -> {} : took {}",
            req.getRequest().getRequestURI(),
            Duration.of(
                System.currentTimeMillis() - req.getValue("start", Long.class),
                TimeUnit.MILLISECONDS
            ).toHumanReadable()
        );
    });

    // previous ActionSteps composition as a new one
    private static ActionStep LoggedAction = LogBefore.andThen(LogAfter);

    @GetMapping("/hello")
    public Action text() {
        // Use composed actionStep
        return LoggedAction.sync(ctx ->
            Ok.text("Hello World!\n")
        );
    }
}
```

## Examples

```java
package example;

import org.reactivecouchbase.common.Duration;
import org.reactivecouchbase.sbessentials.libs.actions.Action;
import org.reactivecouchbase.sbessentials.libs.actions.ActionStep;
import org.reactivecouchbase.sbessentials.libs.ws.WS;
import org.reactivecouchbase.sbessentials.libs.ws.WSResponse;
import org.reactivecouchbase.json.Json;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.concurrent.TimeUnit;

import static org.reactivecouchbase.sbessentials.libs.result.Results.Ok;

@ComponentScan
@SpringBootApplication
public class App {

    public static void main(String[] args) {
        SpringApplication.run(App.class, args);
    }

    @RestController
    @RequestMapping("/api")
    public static class BaseController {

        private static final Logger logger = LoggerFactory.getLogger(BaseController.class);

        // Action that logs before request
        private static ActionStep LogBefore = (req, block) -> {
            Long start = System.currentTimeMillis();
            logger.info("[Log] before action -> {}", req.getRequest().getRequestURI());
            return block.apply(req.setValue("start", start));
        };

        // Action that logs after request
        private static ActionStep LogAfter = (req, block) -> block.apply(req).andThen(ttry -> {
            logger.info(
                "[Log] after action -> {} : took {}",
                req.getRequest().getRequestURI(),
                Duration.of(
                    System.currentTimeMillis() - req.getValue("start", Long.class),
                    TimeUnit.MILLISECONDS
                ).toHumanReadable()
            );
        });

        // Actions composition
        private static ActionStep LoggedAction = LogBefore.andThen(LogAfter);

        @GetMapping("/hello")
        public Action text() {
            // Use composed action
            return LoggedAction.sync(ctx ->
                Ok.text("Hello World!\n")
            );
        }

        @GetMapping("/json")
        public Action json() {
            return Action.sync(ctx ->
                Ok.json(
                    Json.obj().with("message", "Hello World!")
                )
            );
        }

        // Here, async action that fetch a remote webservice and returns the content
        @GetMapping("/ws")
        public Action fetchLocation() {
            return LoggedAction.async(ctx ->
                WS.host("http://freegeoip.net")
                    .withPath("/json/")
                    .call()
                    .flatMap(WSResponse::body)
                    .map(r -> r.json().pretty())
                    .map(p -> Ok.json(p))
            );
        }
    }
}
```

### SSE

```java
package example;

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

        // Stream a json chunk each second forever
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
```

### WebSockets

```java
package example;

import akka.actor.ActorRef;
import akka.actor.Props;
import akka.actor.UntypedActor;
import akka.stream.javadsl.Flow;
import akka.stream.javadsl.Sink;
import akka.stream.javadsl.Source;
import org.reactivecouchbase.json.Json;
import org.reactivecouchbase.sbessentials.libs.websocket.ActorFlow;
import org.reactivecouchbase.sbessentials.libs.websocket.WebSocket;
import org.reactivecouchbase.sbessentials.libs.websocket.WebSocketContext;
import org.reactivecouchbase.sbessentials.libs.websocket.WebSocketMapping;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.socket.WebSocketMessage;
import scala.concurrent.duration.FiniteDuration;

import java.util.concurrent.TimeUnit;

@ComponentScan
@SpringBootApplication
public class WebSocketApp {

    public static void main(String[] args) {
        SpringApplication.run(WebSocketApp.class, args);
    }

    @RestController
    @RequestMapping("/ws")
    public static class WebSocketController {

        private final static Logger logger = LoggerFactory.getLogger(WebSocketController.class);

        // Use a flow to handle input and output
        @WebSocketMapping(path = "/simple")
        public WebSocket simpleWebsocket() {
            return WebSocket.accept(ctx ->
                Flow.fromSinkAndSource(
                    Sink.foreach(msg -> logger.info(msg.getPayload().toString())),
                    Source.tick(
                        FiniteDuration.Zero(),
                        FiniteDuration.create(10, TimeUnit.MILLISECONDS),
                        new org.springframework.web.socket.TextMessage(Json.obj().with("msg", "Hello World!").stringify())
                    )
                )
            );
        }

        // Use an actor to handle input and output
        @WebSocketMapping(path = "/ping")
        public WebSocket webSocketPing() {
            return WebSocket.accept(context ->
                ActorFlow.actorRef(
                    out -> WebsocketPing.props(context, out)
                )
            );
        }
    }

    private static class WebsocketPing extends UntypedActor {

        private final ActorRef out;
        private final WebSocketContext ctx;
        private static final Logger logger = LoggerFactory.getLogger(WebsocketPing.class);

        public WebsocketPing(WebSocketContext ctx, ActorRef out) {
            this.out = out;
            this.ctx = ctx;
        }

        public static Props props(WebSocketContext ctx, ActorRef out) {
            return Props.create(WebsocketPing.class, () -> new WebsocketPing(ctx, out));
        }

        public void onReceive(Object message) throws Exception {
            logger.info("[WebsocketPing] received message from the client {}", message);
            if (message instanceof WebSocketMessage) {
                logger.info("[WebsocketPing] Sending message back the client");
                out.tell(message, getSelf());
            } else {
                unhandled(message);
            }
        }
    }
}
```

## Use it in your project

in your `build.gradle` file


```groovy
repositories {
    mavenCentral()
    maven {
        url 'https://raw.github.com/mathieuancelin/sb-essentials/master/repository/releases/'
    }
}

dependencies {
    compile("org.reactivecouchbase:sb-essentials:0.2.0")
}
```
