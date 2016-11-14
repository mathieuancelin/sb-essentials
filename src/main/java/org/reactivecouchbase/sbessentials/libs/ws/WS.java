package org.reactivecouchbase.sbessentials.libs.ws;


import akka.actor.ActorSystem;
import akka.http.javadsl.Http;
import akka.http.javadsl.OutgoingConnection;
import akka.http.javadsl.model.HttpRequest;
import akka.http.javadsl.model.HttpResponse;
import akka.stream.ActorMaterializer;
import akka.stream.javadsl.Flow;
import akka.stream.javadsl.Sink;
import akka.stream.javadsl.Source;
import org.reactivecouchbase.concurrent.Future;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.context.WebApplicationContext;

import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutorService;

@Component
public class WS {

    static WebApplicationContext webApplicationContext;

    @Autowired
    public void setWebApplicationContext(WebApplicationContext webApplicationContext) {
        WS.webApplicationContext = webApplicationContext;
    }

    static ExecutorService executor() {
        return webApplicationContext.getBean("ws-executor-service", ExecutorService.class);
    }

    static ActorMaterializer materializer() {
        return webApplicationContext.getBean("ws-client-actor-materializer", ActorMaterializer.class);
    }

    static ActorSystem actorSystem() {
        return webApplicationContext.getBean(ActorSystem.class);
    }

    public static Future<WSResponse> call(String host, HttpRequest request) {
        return call(host, request, executor());
    }

    public static Future<WSResponse> call(String host, HttpRequest request, ExecutorService ec) {
        ActorSystem system = WS.actorSystem();
        ActorMaterializer materializer = WS.materializer();
        Flow<HttpRequest, HttpResponse, CompletionStage<OutgoingConnection>> connectionFlow =
                Http.get(system).outgoingConnection(host);
        CompletionStage<HttpResponse> responseFuture =
                Source.single(request)
                        .via(connectionFlow)
                        .runWith(Sink.<HttpResponse>head(), materializer);
        return Future.fromJdkCompletableFuture(responseFuture.toCompletableFuture()).map(WSResponse::new, ec);
    }

    public static WSRequest host(String host) {
        ActorSystem system = WS.actorSystem();
        Flow<HttpRequest, HttpResponse, CompletionStage<OutgoingConnection>> connectionFlow =
                Http.get(system).outgoingConnection(host);
        return new WSRequest(system, connectionFlow, host);
    }
}