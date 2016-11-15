package org.reactivecouchbase.examples.sbessentials;

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