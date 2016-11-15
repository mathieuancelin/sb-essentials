package org.reactivecouchbase.sbessentials.libs.websocket;

import akka.actor.ActorSystem;
import akka.stream.ActorMaterializer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class InternalWebsocketHelper {

    static ActorSystem actorSystem;
    static ActorMaterializer actorMaterializer;

    @Autowired
    public void setActorMaterializer(ActorMaterializer actorMaterializer) {
        InternalWebsocketHelper.actorMaterializer = actorMaterializer;
    }

    @Autowired
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
