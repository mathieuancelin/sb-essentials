package org.reactivecouchbase.sbessentials.libs.websocket;

import akka.actor.ActorSystem;
import akka.stream.ActorMaterializer;
import akka.stream.OverflowStrategy;
import akka.stream.javadsl.Flow;
import akka.stream.javadsl.Sink;
import akka.stream.javadsl.Source;
import akka.stream.javadsl.SourceQueueWithComplete;
import javaslang.control.Option;
import org.reactivecouchbase.concurrent.Future;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

/**
 * Created by adelegue on 13/11/2016.
 */
public class FlowWebSocketHandler extends TextWebSocketHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(FlowWebSocketHandler.class);

    private final ActorSystem system;

    private final ActorMaterializer materializer;

    private final Map<String, SourceQueueWithComplete<String>> connections = new HashMap<>();

    final Function<WebSocketContext, Future<Flow<String, String, ?>>> handler;

    public FlowWebSocketHandler(ActorSystem system, Function<WebSocketContext, Future<Flow<String, String, ?>>> handler) {
        this.system = system;
        this.materializer = ActorMaterializer.create(system);
        this.handler = handler;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        Source<String, SourceQueueWithComplete<String>> queue = Source.queue(50, OverflowStrategy.backpressure());
        Future<Flow<String, String, ?>> flow = handler.apply(new WebSocketContext(session));
        flow.onSuccess(f -> {
            SourceQueueWithComplete<String> matQueue = queue
                    .via(f)
                    .to(Sink.foreach(msg ->
                            session.sendMessage(new TextMessage(msg)))
                    )
                    .run(materializer);
            connections.put(session.getId(), matQueue);
        });
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        get(session.getId()).forEach(queue ->
                queue.offer(message.getPayload())
        );
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        get(session.getId()).forEach(SourceQueueWithComplete::complete);
        connections.remove(session.getId());
    }

    private Option<SourceQueueWithComplete<String>> get(String id) {
        return Option.of(connections.get(id));
    }
}