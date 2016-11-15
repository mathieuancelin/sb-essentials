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
import org.springframework.web.socket.WebSocketMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.AbstractWebSocketHandler;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

/**
 * Created by adelegue on 13/11/2016.
 */
public class FlowWebSocketHandler extends AbstractWebSocketHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(FlowWebSocketHandler.class);

    private final ActorSystem system;

    private final ActorMaterializer materializer;

    private final Map<String, SourceQueueWithComplete<WebSocketMessage>> connections = new HashMap<>();

    final Function<WebSocketContext, Future<Flow<WebSocketMessage, WebSocketMessage, ?>>> handler;

    public FlowWebSocketHandler(ActorSystem system, Function<WebSocketContext, Future<Flow<WebSocketMessage, WebSocketMessage, ?>>> handler) {
        this.system = system;
        this.materializer = ActorMaterializer.create(system);
        this.handler = handler;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        try {
            Source<WebSocketMessage, SourceQueueWithComplete<WebSocketMessage>> queue = Source.queue(50, OverflowStrategy.backpressure());
            Future<Flow<WebSocketMessage, WebSocketMessage, ?>> flow = handler.apply(new WebSocketContext(session));
            flow.onSuccess(f -> {
                SourceQueueWithComplete<WebSocketMessage> matQueue = queue
                    .via(f)
                    .to(Sink.foreach(msg ->
                        session.sendMessage(msg))
                    ).run(materializer);
                matQueue.watchCompletion().thenAccept(done -> {
                    try {
                        session.close(CloseStatus.GOING_AWAY);
                    } catch (Exception e) {
                        LOGGER.error("Error while closing websocket session", e);
                    }
                });
                connections.put(session.getId(), matQueue);
            });
        } catch (Exception e) {
            LOGGER.error("Error after Websocket connection established", e);
        }
    }

    @Override
    public void handleMessage(WebSocketSession session, WebSocketMessage message) throws Exception {
        try {
            get(session.getId()).forEach(queue ->
                queue.offer(message)
            );
        } catch (Exception e) {
            LOGGER.error("Error while handling Websocket message", e);
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        try {
            get(session.getId()).forEach(SourceQueueWithComplete::complete);
            connections.remove(session.getId());
        } catch (Exception e) {
            LOGGER.error("Error after closing Websocket connection", e);
        }
    }

    private Option<SourceQueueWithComplete<WebSocketMessage>> get(String id) {
        return Option.of(connections.get(id));
    }
}