package org.reactivecouchbase.sbessentials.libs.ws;

import akka.actor.ActorSystem;
import akka.http.javadsl.Http;
import akka.http.javadsl.OutgoingConnection;
import akka.http.javadsl.model.*;
import akka.http.javadsl.model.headers.RawHeader;
import akka.stream.javadsl.Flow;
import akka.stream.javadsl.Source;
import akka.util.ByteString;
import javaslang.collection.HashMap;
import javaslang.collection.List;
import javaslang.collection.Map;
import javaslang.collection.Traversable;
import org.reactivecouchbase.common.Duration;
import org.reactivecouchbase.concurrent.Future;
import org.reactivecouchbase.functional.Option;
import org.reactivecouchbase.functional.Tuple;
import org.reactivecouchbase.json.JsValue;

import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutorService;

public class WSRequest {

    private final ActorSystem system;
    private final Flow<HttpRequest, HttpResponse, CompletionStage<OutgoingConnection>> connectionFlow;
    private final String host;
    public final String path;
    public final HttpMethod method;
    public final Source<ByteString, ?> body;
    public final Map<String, List<String>> headers;
    public final Map<String, List<String>> queryString;
    public final Option<Duration> requestTimeout;
    public final Option<Boolean> followsRedirect;
    public final Option<String> virtualHost;
    public final ContentType contentType;

    public WSRequest(ActorSystem system,
                     Flow<HttpRequest, HttpResponse, CompletionStage<OutgoingConnection>> connectionFlow,
                     String host,
                     String path,
                     HttpMethod method,
                     Source<ByteString, ?> body,
                     ContentType contentType,
                     Map<String, List<String>> headers,
                     Map<String, List<String>> queryString,
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
        this.queryString = queryString;
        this.requestTimeout = requestTimeout;
        this.followsRedirect = followsRedirect;
        this.virtualHost = virtualHost;
    }

    public WSRequest(ActorSystem system, Flow<HttpRequest, HttpResponse, CompletionStage<OutgoingConnection>> connectionFlow, String host) {
        this.system = system;
        this.connectionFlow = connectionFlow;
        this.host = host;
        this.path = "/";
        this.method = HttpMethods.GET;
        this.body = Source.empty();
        this.headers = HashMap.empty();
        this.queryString = HashMap.empty();
        this.requestTimeout = Option.none();
        this.followsRedirect = Option.none();
        this.virtualHost = Option.none();
        this.contentType = ContentTypes.TEXT_PLAIN_UTF8;
    }

    public WSRequest withPath(String path) {
        return new WSRequest(system, connectionFlow, host, path, method, body, contentType, headers, queryString, requestTimeout, followsRedirect, virtualHost);
    }

    public WSRequest withMethod(HttpMethod method) {
        return new WSRequest(system, connectionFlow, host, path, method, body, contentType, headers, queryString, requestTimeout, followsRedirect, virtualHost);
    }

    public WSRequest withBody(Source<ByteString, ?> body) {
        return new WSRequest(system, connectionFlow, host, path, method, body, contentType, headers, queryString, requestTimeout, followsRedirect, virtualHost);
    }

    public WSRequest withBody(JsValue body) {
        Source<ByteString, ?> source = Source.single(ByteString.fromString(body.stringify()));
        return new WSRequest(system, connectionFlow, host, path, method, source, ContentTypes.APPLICATION_JSON, headers, queryString, requestTimeout, followsRedirect, virtualHost);
    }

    public WSRequest withBody(String body) {
        Source<ByteString, ?> source = Source.single(ByteString.fromString(body));
        return new WSRequest(system, connectionFlow, host, path, method, source, ContentTypes.TEXT_PLAIN_UTF8, headers, queryString, requestTimeout, followsRedirect, virtualHost);
    }

    public WSRequest withHeaders(Map<String, List<String>> headers) {
        return new WSRequest(system, connectionFlow, host, path, method, body, contentType, headers, queryString, requestTimeout, followsRedirect, virtualHost);
    }

    public WSRequest withHeader(String name, String value) {
        Map<String, List<String>> _headers = headers;
        if (!_headers.containsKey(name)) {
            _headers = _headers.put(name, List.of(value));
        } else {
            _headers = _headers.put(name, _headers.get(name).get().append(value));
        }
        return new WSRequest(system, connectionFlow, host, path, method, body, contentType, _headers, queryString, requestTimeout, followsRedirect, virtualHost);
    }

    public WSRequest withQueryString(Map<String, List<String>> queryString) {
        return new WSRequest(system, connectionFlow, host, path, method, body, contentType, headers, queryString, requestTimeout, followsRedirect, virtualHost);
    }

    public WSRequest withQueryParam(String name, String value) {
        Map<String, List<String>> _queryString = queryString;
        if (!_queryString.containsKey(name)) {
            _queryString = _queryString.put(name, List.of(value));
        } else {
            _queryString = _queryString.put(name, _queryString.get(name).get().append(value));
        }
        return new WSRequest(system, connectionFlow, host, path, method, body, contentType, headers, _queryString, requestTimeout, followsRedirect, virtualHost);
    }

    public WSRequest withRequestTimeout(Option<Duration> requestTimeout) {
        return new WSRequest(system, connectionFlow, host, path, method, body, contentType, headers, queryString, requestTimeout, followsRedirect, virtualHost);
    }

    public WSRequest withFollowsRedirect(Option<Boolean> followsRedirect) {
        return new WSRequest(system, connectionFlow, host, path, method, body, contentType, headers, queryString, requestTimeout, followsRedirect, virtualHost);
    }

    public WSRequest withVirtualHost(Option<String> virtualHost) {
        return new WSRequest(system, connectionFlow, host, path, method, body, contentType, headers, queryString, requestTimeout, followsRedirect, virtualHost);
    }

    public Future<WSResponse> call() {
        String _queryString = queryString.toList().flatMap(tuple -> tuple._2.map(v -> tuple._1 + "=" + v)).mkString("&");
        List<HttpHeader> _headers = headers.toList().flatMap(tuple -> tuple._2.map(v -> RawHeader.create(tuple._1, v)));
        HttpRequest request = HttpRequest.create(path + (queryString.isEmpty() ? "" : _queryString));
        // TODO : handle requestTimeout
        // TODO : handle followsRedirect
        // TODO : handle virtualHost
        return WS.call(host, request.withMethod(method)
                .withEntity(HttpEntities.createChunked(contentType, body))
                .addHeaders(_headers));
    }
}
