package org.reactivecouchbase.sbessentiels.tests;

import akka.actor.ActorRef;
import akka.actor.PoisonPill;
import akka.actor.Props;
import akka.actor.UntypedActor;
import akka.http.javadsl.model.ws.Message;
import akka.http.javadsl.model.ws.TextMessage;
import javaslang.collection.List;
import org.reactivecouchbase.concurrent.Promise;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import scala.concurrent.duration.FiniteDuration;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class WebSocketClientActor extends UntypedActor {

    private final ActorRef out;

    private static final Logger logger = LoggerFactory.getLogger(WebSocketClientActor.class);

    private AtomicInteger count = new AtomicInteger(0);

    private List<Message> messages = List.empty();

    private Promise<List<Message>> promise;

    public WebSocketClientActor(ActorRef out, Promise<List<akka.http.javadsl.model.ws.Message>> promise) {
        this.out = out;
        this.promise = promise;
    }

    public static Props props(ActorRef out, Promise<List<akka.http.javadsl.model.ws.Message>> promise) {
        return Props.create(WebSocketClientActor.class, () -> new WebSocketClientActor(out, promise));
    }

    @Override
    public void preStart() {
        ActorRef self = getSelf();
        getContext().system().scheduler().schedule(FiniteDuration.Zero(), FiniteDuration.apply(100, TimeUnit.MILLISECONDS), () -> {
            self.tell(TextMessage.create("chunk"), ActorRef.noSender());
        }, context().dispatcher());
    }

    public void onReceive(Object message) throws Exception {
        logger.info("[WebSocketClientActor] received message {}", message);
        if (message != null && akka.http.javadsl.model.ws.Message.class.isAssignableFrom(message.getClass())) {
            if (count.get() == 10) {
                promise.trySuccess(messages);
                out.tell(PoisonPill.getInstance(), ActorRef.noSender());
                getSelf().tell(PoisonPill.getInstance(), ActorRef.noSender());
            } else {
                logger.info("[WebSocketClientActor] Sending a chunk {}", count.get());
                count.incrementAndGet();
                messages = messages.append((akka.http.javadsl.model.ws.Message) message);
                out.tell(TextMessage.create("chunk"), getSelf());
            }
        } else {
            unhandled(message);
        }
    }
}
