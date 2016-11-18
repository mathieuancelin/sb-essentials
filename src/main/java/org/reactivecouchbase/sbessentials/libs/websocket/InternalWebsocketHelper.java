package org.reactivecouchbase.sbessentials.libs.websocket;

import akka.actor.ActorSystem;
import akka.stream.ActorMaterializer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

@Component
class InternalWebsocketHelper {

    private static ActorSystem actorSystem;
    
    private static ActorMaterializer actorMaterializer;

    @Autowired @Qualifier("websocket-actor-materializer")
    public void setActorMaterializer(ActorMaterializer actorMaterializer) {
        InternalWebsocketHelper.actorMaterializer = actorMaterializer;
    }

    @Autowired @Qualifier("websocket-actor-system")
    public void setActorSystem(ActorSystem actorSystem) {
        InternalWebsocketHelper.actorSystem = actorSystem;
    }

    static ActorSystem actorSystem() {
        return actorSystem;
    }

    static ActorMaterializer actorMaterializer() {
        return actorMaterializer;
    }
}
