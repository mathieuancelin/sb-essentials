package org.reactivecouchbase.sbessentials.libs.ws;

import akka.actor.ActorSystem;
import akka.http.javadsl.OutgoingConnection;
import akka.http.javadsl.model.*;
import akka.http.javadsl.model.headers.RawHeader;
import akka.stream.ActorMaterializer;
import akka.stream.javadsl.Flow;
import akka.stream.javadsl.Sink;
import akka.stream.javadsl.Source;
import akka.stream.javadsl.StreamConverters;
import akka.util.ByteString;
import javaslang.collection.HashMap;
import javaslang.collection.List;
import javaslang.collection.Map;
import org.reactivecouchbase.common.Duration;
import org.reactivecouchbase.concurrent.Future;
import org.reactivecouchbase.functional.Option;
import org.reactivecouchbase.json.JsValue;

import java.io.InputStream;
import java.util.concurrent.CompletionStage;

public class WSRequest {

    private final ActorSystem system;
    private final Flow<HttpRequest, HttpResponse, CompletionStage<OutgoingConnection>> connectionFlow;
    private final String host;
    private final String path;
    private final HttpMethod method;
    private final Source<ByteString, ?> body;
    private final Map<String, List<String>> headers;
    private final Map<String, List<String>> queryParams;
    private final Option<Duration> requestTimeout;
    private final Option<Boolean> followsRedirect;
    private final Option<String> virtualHost;
    private final ContentType contentType;
    private final ActorMaterializer materializer;

    WSRequest(ActorSystem system,
              ActorMaterializer materializer,
              Flow<HttpRequest, HttpResponse, CompletionStage<OutgoingConnection>> connectionFlow,
              String host,
              String path,
              HttpMethod method,
              Source<ByteString, ?> body,
              ContentType contentType,
              Map<String, List<String>> headers,
              Map<String, List<String>> queryParams,
              Option<Duration> requestTimeout,
              Option<Boolean> followsRedirect,
              Option<String> virtualHost) {
        this.system = system;
        this.connectionFlow = connectionFlow;
        this.host = host;
        this.path = path;
        this.method = method;
        this.body = body;
        this.headers = headers;
        this.contentType = contentType;
        this.queryParams = queryParams;
        this.requestTimeout = requestTimeout;
        this.followsRedirect = followsRedirect;
        this.virtualHost = virtualHost;
        this.materializer = materializer;
    }

    WSRequest(ActorSystem system, Flow<HttpRequest, HttpResponse, CompletionStage<OutgoingConnection>> connectionFlow, String host) {
        this.system = system;
        this.connectionFlow = connectionFlow;
        this.host = host;
        this.path = "/";
        this.method = HttpMethods.GET;
        this.body = Source.empty();
        this.headers = HashMap.empty();
        this.queryParams = HashMap.empty();
        this.requestTimeout = Option.none();
        this.followsRedirect = Option.none();
        this.virtualHost = Option.none();
        this.contentType = ContentTypes.TEXT_PLAIN_UTF8;
        //this.materializer = ActorMaterializer.create(system);
        this.materializer = WS.webApplicationContext.getBean(ActorMaterializer.class);

    }

    public WSRequest withPath(String path) {
        return new WSRequest(system, materializer, connectionFlow, host, path, method, body, contentType, headers, queryParams, requestTimeout, followsRedirect, virtualHost);
    }

    public WSRequest addPathSegment(String path) {
        return new WSRequest(system, materializer, connectionFlow, host, this.path + "/" + path, method, body, contentType, headers, queryParams, requestTimeout, followsRedirect, virtualHost);
    }

    public WSRequest withMethod(HttpMethod method) {
        return new WSRequest(system, materializer, connectionFlow, host, path, method, body, contentType, headers, queryParams, requestTimeout, followsRedirect, virtualHost);
    }

    public WSRequest withBody(Source<ByteString, ?> body) {
        return new WSRequest(system, materializer, connectionFlow, host, path, method, body, contentType, headers, queryParams, requestTimeout, followsRedirect, virtualHost);
    }

    public WSRequest withBody(Source<ByteString, ?> body, ContentType ctype) {
        return new WSRequest(system, materializer, connectionFlow, host, path, method, body, ctype, headers, queryParams, requestTimeout, followsRedirect, virtualHost);
    }

    public WSRequest withBody(JsValue body) {
        Source<ByteString, ?> source = Source.single(ByteString.fromString(body.stringify()));
        return new WSRequest(system, materializer, connectionFlow, host, path, method, source, ContentTypes.APPLICATION_JSON, headers, queryParams, requestTimeout, followsRedirect, virtualHost);
    }

    public WSRequest withBody(String body) {
        Source<ByteString, ?> source = Source.single(ByteString.fromString(body));
        return new WSRequest(system, materializer, connectionFlow, host, path, method, source, ContentTypes.TEXT_PLAIN_UTF8, headers, queryParams, requestTimeout, followsRedirect, virtualHost);
    }

    public WSRequest withBody(String body, ContentType ctype) {
        Source<ByteString, ?> source = Source.single(ByteString.fromString(body));
        return new WSRequest(system, materializer, connectionFlow, host, path, method, source, ctype, headers, queryParams, requestTimeout, followsRedirect, virtualHost);
    }

    public WSRequest withBody(ByteString body) {
        Source<ByteString, ?> source = Source.single(body);
        return new WSRequest(system, materializer, connectionFlow, host, path, method, source, ContentTypes.TEXT_PLAIN_UTF8, headers, queryParams, requestTimeout, followsRedirect, virtualHost);
    }

    public WSRequest withBody(ByteString body, ContentType ctype) {
        Source<ByteString, ?> source = Source.single(body);
        return new WSRequest(system, materializer, connectionFlow, host, path, method, source, ctype, headers, queryParams, requestTimeout, followsRedirect, virtualHost);
    }

    public WSRequest withBody(byte[] body) {
        Source<ByteString, ?> source = Source.single(ByteString.fromArray(body));
        return new WSRequest(system, materializer, connectionFlow, host, path, method, source, ContentTypes.TEXT_PLAIN_UTF8, headers, queryParams, requestTimeout, followsRedirect, virtualHost);
    }

    public WSRequest withBody(byte[] body, ContentType ctype) {
        Source<ByteString, ?> source = Source.single(ByteString.fromArray(body));
        return new WSRequest(system, materializer, connectionFlow, host, path, method, source, ctype, headers, queryParams, requestTimeout, followsRedirect, virtualHost);
    }

    public WSRequest withBody(InputStream body) {
        Source<ByteString, ?> source = StreamConverters.fromInputStream(() -> body);
        return new WSRequest(system, materializer, connectionFlow, host, path, method, source, ContentTypes.APPLICATION_OCTET_STREAM, headers, queryParams, requestTimeout, followsRedirect, virtualHost);
    }

    public WSRequest withBody(InputStream body, ContentType ctype) {
        Source<ByteString, ?> source = StreamConverters.fromInputStream(() -> body);
        return new WSRequest(system, materializer, connectionFlow, host, path, method, source, ctype, headers, queryParams, requestTimeout, followsRedirect, virtualHost);
    }

    public WSRequest withHeaders(Map<String, List<String>> headers) {
        return new WSRequest(system, materializer, connectionFlow, host, path, method, body, contentType, headers, queryParams, requestTimeout, followsRedirect, virtualHost);
    }

    public WSRequest withHeader(String name, String value) {
        Map<String, List<String>> _headers = headers;
        if (!_headers.containsKey(name)) {
            _headers = _headers.put(name, List.of(value));
        } else {
            _headers = _headers.put(name, _headers.get(name).get().append(value));
        }
        return new WSRequest(system, materializer, connectionFlow, host, path, method, body, contentType, _headers, queryParams, requestTimeout, followsRedirect, virtualHost);
    }

    public WSRequest withQueryParams(Map<String, List<String>> queryString) {
        return new WSRequest(system, materializer, connectionFlow, host, path, method, body, contentType, headers, queryString, requestTimeout, followsRedirect, virtualHost);
    }

    public WSRequest withQueryParam(String name, String value) {
        Map<String, List<String>> _queryString = queryParams;
        if (!_queryString.containsKey(name)) {
            _queryString = _queryString.put(name, List.of(value));
        } else {
            _queryString = _queryString.put(name, _queryString.get(name).get().append(value));
        }
        return new WSRequest(system, materializer, connectionFlow, host, path, method, body, contentType, headers, _queryString, requestTimeout, followsRedirect, virtualHost);
    }

    // public WSRequest withRequestTimeout(Option<Duration> requestTimeout) {
    //     return new WSRequest(system, materializer, connectionFlow, host, path, method, body, contentType, headers, queryParams, requestTimeout, followsRedirect, virtualHost);
    // }
    // public WSRequest withFollowsRedirect(Option<Boolean> followsRedirect) {
    //     return new WSRequest(system, materializer, connectionFlow, host, path, method, body, contentType, headers, queryParams, requestTimeout, followsRedirect, virtualHost);
    // }
    // public WSRequest withVirtualHost(Option<String> virtualHost) {
    //     return new WSRequest(system, materializer, connectionFlow, host, path, method, body, contentType, headers, queryParams, requestTimeout, followsRedirect, virtualHost);
    // }

    // TODO : handle requestTimeout
    // TODO : handle followsRedirect
    // TODO : handle virtualHost
    public Future<WSResponse> call() {
        String _queryString = queryParams.toList().flatMap(tuple -> tuple._2.map(v -> tuple._1 + "=" + v)).mkString("&");
        List<HttpHeader> _headers = headers.toList().flatMap(tuple -> tuple._2.map(v -> RawHeader.create(tuple._1, v)));
        HttpRequest request = HttpRequest.create(path + (queryParams.isEmpty() ? "" : "?" + _queryString))
            .withMethod(method)
            .withEntity(HttpEntities.createChunked(contentType, body))
            .addHeaders(_headers);
        CompletionStage<HttpResponse> responseFuture = Source.single(request).via(connectionFlow).runWith(Sink.head(), materializer);
        return Future.fromJdkCompletableFuture(responseFuture.toCompletableFuture()).map(WSResponse::new);
    }
}
