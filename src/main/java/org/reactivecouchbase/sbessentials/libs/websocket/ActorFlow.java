package org.reactivecouchbase.sbessentials.libs.websocket;

import akka.actor.*;
import akka.japi.Creator;
import akka.japi.Pair;
import akka.stream.Materializer;
import akka.stream.OverflowStrategy;
import akka.stream.javadsl.*;
import org.reactivestreams.Publisher;

import java.util.function.Function;

public class ActorFlow {

    public static <In, Out> Flow<In, Out, ?> actorRef(
            Function<ActorRef, Props> props,
            int bufferSize,
            OverflowStrategy overflowStrategy,
            ActorRefFactory factory,
            Materializer mat) {

        Pair<ActorRef, Publisher<Out>> pair =
                Source.<Out>actorRef(bufferSize, overflowStrategy).toMat(Sink.asPublisher(AsPublisher.WITHOUT_FANOUT), Keep.both()).run(mat);

        Creator<Actor> creator = (Creator<Actor>) () -> new UntypedActor() {
            private ActorRef flowActor = context().watch(context().actorOf(props.apply(pair.first()), "flowActor"));
            @Override
            public void onReceive(Object message) throws Throwable {
                if (message instanceof Status.Success) {
                    flowActor.tell(PoisonPill.getInstance(), getSelf());
                } else if (message instanceof Terminated) {
                    context().stop(getSelf());
                } else {
                    flowActor.tell(message, getSelf());
                }
            }

            @Override
            public SupervisorStrategy supervisorStrategy() {
                return SupervisorStrategy.stoppingStrategy();
            }
        };

        return Flow.fromSinkAndSource(
            Sink.actorRef(
                factory.actorOf(Props.create(creator)),
                new Status.Success(new Object())
            ),
            Source.fromPublisher(pair.second())
        );
    }
}
