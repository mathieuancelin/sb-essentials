package org.reactivecouchbase.sbessentiels.tests;

import akka.actor.ActorRef;
import akka.actor.Props;
import akka.actor.UntypedActor;
import org.reactivecouchbase.json.JsValue;
import org.reactivecouchbase.json.Json;
import org.reactivecouchbase.sbessentials.libs.websocket.WebSocketContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MyWebSocketActor extends UntypedActor {

    private final ActorRef out;
    private final WebSocketContext ctx;
    private static final Logger logger = LoggerFactory.getLogger(WebsocketPing.class);

    public MyWebSocketActor(WebSocketContext ctx, ActorRef out) {
        this.out = out;
        this.ctx = ctx;
    }

    public static Props props(WebSocketContext ctx, ActorRef out) {
        return Props.create(MyWebSocketActor.class, () -> new MyWebSocketActor(ctx, out));
    }

    public void onReceive(Object message) throws Exception {
        logger.info("[MyWebSocketActor] received message {}", message);
        if (message instanceof org.springframework.web.socket.TextMessage) {
            JsValue value = Json.parse(((org.springframework.web.socket.TextMessage) message).getPayload());
            JsValue response = Json.obj()
                    .with("sent_at", System.currentTimeMillis())
                    .with("resource", ctx.pathParam("id").getOrElse("No value !!!"))
                    .with("sourceMessage", value);
            out.tell(new org.springframework.web.socket.TextMessage(response.stringify()), getSelf());
        } else {
            unhandled(message);
        }
    }
}