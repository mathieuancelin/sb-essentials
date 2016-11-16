package org.reactivecouchbase.sbessentiels.tests;

import akka.actor.ActorRef;
import akka.actor.Props;
import akka.japi.pf.ReceiveBuilder;
import akka.stream.actor.AbstractActorPublisher;
import akka.stream.actor.ActorPublisherMessage;
import akka.stream.javadsl.Source;

public class SSEActor extends AbstractActorPublisher<String> {

    public static Props props() {
        return Props.create(SSEActor.class, () -> new SSEActor());
    }

    public static Source<String, ActorRef> source() {
        return Source.actorPublisher(props());
    }

    private int total = 0;

    public SSEActor() {
        receive(ReceiveBuilder.
            matchEquals("START", val -> {
                System.out.println("Received Start");
            }).
            match(ActorPublisherMessage.Request.class, request -> {
                System.out.println("Ask for " + totalDemand());
                for (int i = 0; i < totalDemand() && i < 3; i++) {
                    total++;
                    onNext("data: {\"Hello\": \"World!\"}\n\n");
                }
                if (total == 3) {
                    onComplete();
                }
            }).
            match(ActorPublisherMessage.Cancel.class, cancel -> context().stop(self())).
            build());
    }
}