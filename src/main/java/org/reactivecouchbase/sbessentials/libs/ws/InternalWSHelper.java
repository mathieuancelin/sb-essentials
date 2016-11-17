package org.reactivecouchbase.sbessentials.libs.ws;

import akka.actor.ActorSystem;
import akka.stream.ActorMaterializer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.context.WebApplicationContext;

import java.util.concurrent.ExecutorService;

@Component
class InternalWSHelper {

    static WebApplicationContext webApplicationContext;

    @Autowired
    public void setWebApplicationContext(WebApplicationContext webApplicationContext) {
        InternalWSHelper.webApplicationContext = webApplicationContext;
    }

    static ExecutorService executor() {
        return webApplicationContext.getBean("ws-executor-service", ExecutorService.class);
    }

    static ActorMaterializer materializer() {
        return webApplicationContext.getBean("ws-client-actor-materializer", ActorMaterializer.class);
    }

    static ActorSystem actorSystem() {
        return webApplicationContext.getBean("ws-actor-system", ActorSystem.class);
    }

}
