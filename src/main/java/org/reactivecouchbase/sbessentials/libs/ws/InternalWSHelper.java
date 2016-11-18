package org.reactivecouchbase.sbessentials.libs.ws;

import akka.actor.ActorSystem;
import akka.http.javadsl.Http;
import akka.stream.ActorMaterializer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.context.WebApplicationContext;

import java.util.concurrent.ExecutorService;

@Component
class InternalWSHelper {

    // static WebApplicationContext webApplicationContext;

    // @Autowired
    // public void setWebApplicationContext(WebApplicationContext webApplicationContext) {
    //     InternalWSHelper.webApplicationContext = webApplicationContext;
    // }
    // return webApplicationContext.getBean("ws-executor-service", ExecutorService.class);
    // return webApplicationContext.getBean("ws-client-actor-materializer", ActorMaterializer.class);
    // return webApplicationContext.getBean("ws-actor-system", ActorSystem.class);

    private static ActorSystem wsActorSystem;
    private static ActorMaterializer wsActorMaterializer;
    private static ExecutorService wsExecutorService;
    private static Http wsHttp;

    private static ActorSystem websocketActorSystem;
    private static ActorMaterializer websocketActorMaterializer;
    private static ExecutorService websocketExecutorService;
    private static Http websocketHttp;

    @Autowired @Qualifier("ws-executor-service")
    public void setWSExecutorService(ExecutorService executorService) {
        InternalWSHelper.wsExecutorService = executorService;
    }

    @Autowired @Qualifier("ws-client-actor-materializer")
    public void setWSActorMaterializer(ActorMaterializer actorMaterializer) {
        InternalWSHelper.wsActorMaterializer = actorMaterializer;
    }

    @Autowired @Qualifier("ws-actor-system")
    public void setWSActorSystem(ActorSystem actorSystem) {
        InternalWSHelper.wsActorSystem = actorSystem;
    }

    @Autowired @Qualifier("ws-http")
    public void setWSActorSystem(Http http) {
        InternalWSHelper.wsHttp = http;
    }

    @Autowired @Qualifier("websocket-executor-service")
    public void setWebSocketExecutorService(ExecutorService executorService) {
        InternalWSHelper.websocketExecutorService = executorService;
    }

    @Autowired @Qualifier("websocket-actor-materializer")
    public void setWebSocketActorMaterializer(ActorMaterializer actorMaterializer) {
        InternalWSHelper.websocketActorMaterializer = actorMaterializer;
    }

    @Autowired @Qualifier("websocket-actor-system")
    public void setWebSocketActorSystem(ActorSystem actorSystem) {
        InternalWSHelper.websocketActorSystem = actorSystem;
    }

    @Autowired @Qualifier("websocket-http")
    public void setWebSocketActorSystem(Http http) {
        InternalWSHelper.websocketHttp = http;
    }

    static ExecutorService wsExecutor() {
        return wsExecutorService;
    }

    static ActorMaterializer wsMaterializer() {
        return wsActorMaterializer;
    }

    static ActorSystem wsActorSystem() {
        return wsActorSystem;
    }

    static Http wsHttp() {
        return wsHttp;
    }

    static ExecutorService websocketExecutor() {
        return websocketExecutorService;
    }

    static ActorMaterializer websocketMaterializer() {
        return websocketActorMaterializer;
    }

    static ActorSystem websocketActorSystem() {
        return websocketActorSystem;
    }

    static Http websocketHttp() {
        return websocketHttp;
    }
}
