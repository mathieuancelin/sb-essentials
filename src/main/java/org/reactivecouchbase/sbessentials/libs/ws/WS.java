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

import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutorService;

public class WS {

    public static Future<WSResponse> call(String host, HttpRequest request) {
        return call(host, request, InternalWSHelper.executor());
    }

    public static Future<WSResponse> call(String host, HttpRequest request, ExecutorService ec) {
        ActorSystem system = InternalWSHelper.actorSystem();
        ActorMaterializer materializer = InternalWSHelper.materializer();
        Flow<HttpRequest, HttpResponse, CompletionStage<OutgoingConnection>> connectionFlow =
                Http.get(system).outgoingConnection(host);
        CompletionStage<HttpResponse> responseFuture =
                Source.single(request)
                        .via(connectionFlow)
                        .runWith(Sink.<HttpResponse>head(), materializer);
        return Future.fromJdkCompletableFuture(responseFuture.toCompletableFuture()).map(WSResponse::new, ec);
    }

    public static WSRequest host(String host) {
        ActorSystem system = InternalWSHelper.actorSystem();
        Flow<HttpRequest, HttpResponse, CompletionStage<OutgoingConnection>> connectionFlow =
                Http.get(system).outgoingConnection(host);
        return new WSRequest(system, connectionFlow, host);
    }

    public static WebSocketClientRequest websocketHost(String host) {
        ActorSystem system = InternalWSHelper.actorSystem();
        ActorMaterializer materializer = InternalWSHelper.materializer();
        return new WebSocketClientRequest(system, materializer, Http.get(system), host, "");
    }
}