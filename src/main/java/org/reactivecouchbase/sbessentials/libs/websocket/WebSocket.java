package org.reactivecouchbase.sbessentials.libs.websocket;

import akka.stream.javadsl.Flow;
import javaslang.concurrent.Future;
import org.springframework.web.socket.WebSocketMessage;

import java.util.function.Function;

public class WebSocket {

    public final Function<WebSocketContext, Future<Flow<WebSocketMessage, WebSocketMessage, ?>>> handler;

    private WebSocket(Function<WebSocketContext, Future<Flow<WebSocketMessage, WebSocketMessage, ?>>> handler) {
        this.handler = handler;
    }

    public static WebSocket accept(Function<WebSocketContext, Flow<WebSocketMessage, WebSocketMessage, ?>> handler) {
        return new WebSocket(ctx -> Future.successful(handler.apply(ctx)));
    }

    public static WebSocket acceptAsync(Function<WebSocketContext, Future<Flow<WebSocketMessage, WebSocketMessage, ?>>>  handler) {
        return new WebSocket(handler);
    }
}
