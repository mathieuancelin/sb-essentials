package org.reactivecouchbase.sbessentials.libs.ws;

import akka.actor.ActorSystem;
import akka.http.javadsl.ConnectHttp;
import akka.http.javadsl.ConnectionContext;
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
        return call(host, request, InternalWSHelper.wsExecutor());
    }

    public static Future<WSResponse> call(String host, HttpRequest request, ExecutorService ec) {
        return call(ConnectHttp.toHost(host), request, ec);
    }

    public static <T extends ConnectHttp> Future<WSResponse> call(T host, HttpRequest request, ExecutorService ec) {
        ActorMaterializer materializer = InternalWSHelper.wsMaterializer();
        Flow<HttpRequest, HttpResponse, CompletionStage<OutgoingConnection>> connectionFlow =
                InternalWSHelper.wsHttp().outgoingConnection(host);
        CompletionStage<HttpResponse> responseFuture =
                Source.single(request)
                        .via(connectionFlow)
                        .runWith(Sink.<HttpResponse>head(), materializer);
        return Future.fromJdkCompletableFuture(responseFuture.toCompletableFuture()).map(WSResponse::new, ec);
    }

    public static WSRequest host(String host) {
        return host(ConnectHttp.toHost(host));
    }

    public static <T extends ConnectHttp> WSRequest host(T host) {
        ActorSystem system = InternalWSHelper.wsActorSystem();
        Flow<HttpRequest, HttpResponse, CompletionStage<OutgoingConnection>> connectionFlow =
                InternalWSHelper.wsHttp().outgoingConnection(host);
        return new WSRequest(system, connectionFlow, host.host());
    }

    public static WebSocketClientRequest websocketHost(String host) {
        ActorSystem system = InternalWSHelper.websocketActorSystem();
        ActorMaterializer materializer = InternalWSHelper.websocketMaterializer();
        return new WebSocketClientRequest(system, materializer, InternalWSHelper.websocketHttp(), host, "");
    }
}