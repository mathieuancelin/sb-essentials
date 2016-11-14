package org.reactivecouchbase.sbessentials.libs.websocket;

import akka.stream.javadsl.Flow;
import org.reactivecouchbase.concurrent.Future;

import java.util.function.Function;

public class WebSocket {

    public final Function<WebSocketContext, Future<Flow<String, String, ?>>> handler;

    private WebSocket(Function<WebSocketContext, Future<Flow<String, String, ?>>> handler) {
        this.handler = handler;
    }

    public static WebSocket accept(Function<WebSocketContext, Flow<String, String, ?>> handler) {
        return new WebSocket(ctx -> Future.successful(handler.apply(ctx)));
    }

    public static WebSocket acceptAsync(Function<WebSocketContext, Future<Flow<String, String, ?>>>  handler) {
        return new WebSocket(handler);
    }
}
