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
import org.reactivestreams.Publisher;

import java.io.InputStream;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutorService;

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
        this.materializer = WS.materializer();

    }

    private WSRequest(Builder builder) {
        system = builder.system;
        connectionFlow = builder.connectionFlow;
        host = builder.host;
        path = builder.path;
        method = builder.method;
        body = builder.body;
        headers = builder.headers;
        queryParams = builder.queryParams;
        requestTimeout = builder.requestTimeout;
        followsRedirect = builder.followsRedirect;
        virtualHost = builder.virtualHost;
        contentType = builder.contentType;
        materializer = builder.materializer;
    }

    public static Builder newBuilder() {
        return new Builder();
    }

    public static Builder newBuilder(WSRequest copy) {
        Builder builder = new Builder();
        builder.system = copy.system;
        builder.connectionFlow = copy.connectionFlow;
        builder.host = copy.host;
        builder.path = copy.path;
        builder.method = copy.method;
        builder.body = copy.body;
        builder.headers = copy.headers;
        builder.queryParams = copy.queryParams;
        builder.requestTimeout = copy.requestTimeout;
        builder.followsRedirect = copy.followsRedirect;
        builder.virtualHost = copy.virtualHost;
        builder.contentType = copy.contentType;
        builder.materializer = copy.materializer;
        return builder;
    }

    public Builder copy() {
        return newBuilder(this);
    }

    public WSRequest withPath(String path) {
        return copy().withPath(path).build();
    }

    public WSRequest addPathSegment(String path) {
        return copy().withPath(this.path + "/" + path).build();
    }

    public WSRequest withMethod(HttpMethod method) {
        return copy().withMethod(method).build();
    }

    public WSRequest withBody(Publisher<ByteString> body) {
        return withBody(Source.fromPublisher(body));
    }

    public WSRequest withBody(Source<ByteString, ?> body) {
        return copy().withBody(body).build();
    }

    public WSRequest withBody(Publisher<ByteString> body, ContentType ctype) {
        return withBody(Source.fromPublisher(body), ctype);
    }

    public WSRequest withBody(Source<ByteString, ?> body, ContentType ctype) {
        return copy().withBody(body).withContentType(ctype).build();
    }

    public WSRequest withBody(JsValue body) {
        Source<ByteString, ?> source = Source.single(ByteString.fromString(body.stringify()));
        return copy().withBody(source).withContentType(ContentTypes.APPLICATION_JSON).build();
    }

    public WSRequest withBody(String body) {
        Source<ByteString, ?> source = Source.single(ByteString.fromString(body));
        return copy().withBody(source).withContentType(ContentTypes.TEXT_PLAIN_UTF8).build();
    }

    public WSRequest withBody(String body, ContentType ctype) {
        Source<ByteString, ?> source = Source.single(ByteString.fromString(body));
        return copy().withBody(source).withContentType(ctype).build();
    }

    public WSRequest withBody(ByteString body) {
        Source<ByteString, ?> source = Source.single(body);
        return copy().withBody(source).withContentType(ContentTypes.TEXT_PLAIN_UTF8).build();
    }

    public WSRequest withBody(ByteString body, ContentType ctype) {
        Source<ByteString, ?> source = Source.single(body);
        return copy().withBody(source).withContentType(ctype).build();
    }

    public WSRequest withBody(byte[] body) {
        Source<ByteString, ?> source = Source.single(ByteString.fromArray(body));
        return copy().withBody(source).withContentType(ContentTypes.TEXT_PLAIN_UTF8).build();
    }

    public WSRequest withBody(byte[] body, ContentType ctype) {
        Source<ByteString, ?> source = Source.single(ByteString.fromArray(body));
        return copy().withBody(source).withContentType(ctype).build();
    }

    public WSRequest withBody(InputStream body) {
        Source<ByteString, ?> source = StreamConverters.fromInputStream(() -> body);
        return copy().withBody(source).withContentType(ContentTypes.APPLICATION_OCTET_STREAM).build();
    }

    public WSRequest withBody(InputStream body, ContentType ctype) {
        Source<ByteString, ?> source = StreamConverters.fromInputStream(() -> body);
        return copy().withBody(source).withContentType(ctype).build();
    }

    public WSRequest withHeaders(Map<String, List<String>> headers) {
        return copy().withHeaders(headers).build();
    }

    public WSRequest withHeader(String name, String value) {
        Map<String, List<String>> _headers = headers;
        if (!_headers.containsKey(name)) {
            _headers = _headers.put(name, List.of(value));
        } else {
            _headers = _headers.put(name, _headers.get(name).get().append(value));
        }
        return copy().withHeaders(_headers).build();
        // return new WSRequest(system, materializer, connectionFlow, host, path, method, body, contentType, _headers, queryParams, requestTimeout, followsRedirect, virtualHost);
    }

    public WSRequest withQueryParams(Map<String, List<String>> queryString) {
        return copy().withQueryParams(queryString).build();
    }

    public WSRequest withQueryParam(String name, String value) {
        Map<String, List<String>> _queryString = queryParams;
        if (!_queryString.containsKey(name)) {
            _queryString = _queryString.put(name, List.of(value));
        } else {
            _queryString = _queryString.put(name, _queryString.get(name).get().append(value));
        }
        return copy().withQueryParams(_queryString).build();
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
        return call(WS.executor());
    }

    public Future<WSResponse> call(ExecutorService ec) {
        String _queryString = queryParams.toList().flatMap(tuple -> tuple._2.map(v -> tuple._1 + "=" + v)).mkString("&");
        List<HttpHeader> _headers = headers.toList().flatMap(tuple -> tuple._2.map(v -> RawHeader.create(tuple._1, v)));
        HttpRequest request = HttpRequest.create(path + (queryParams.isEmpty() ? "" : "?" + _queryString))
            .withMethod(method)
            .withEntity(HttpEntities.createChunked(contentType, body))
            .addHeaders(_headers);
        CompletionStage<HttpResponse> responseFuture = Source.single(request).via(connectionFlow).runWith(Sink.head(), materializer);
        return Future.fromJdkCompletableFuture(responseFuture.toCompletableFuture()).map(WSResponse::new, ec);
    }


    public static final class Builder {
        private ActorSystem system;
        private Flow<HttpRequest, HttpResponse, CompletionStage<OutgoingConnection>> connectionFlow;
        private String host;
        private String path;
        private HttpMethod method;
        private Source<ByteString, ?> body;
        private Map<String, List<String>> headers;
        private Map<String, List<String>> queryParams;
        private Option<Duration> requestTimeout;
        private Option<Boolean> followsRedirect;
        private Option<String> virtualHost;
        private ContentType contentType;
        private ActorMaterializer materializer;

        private Builder() {
        }

        public Builder withHost(String val) {
            host = val;
            return this;
        }

        public Builder withPath(String val) {
            path = val;
            return this;
        }

        public Builder withMethod(HttpMethod val) {
            method = val;
            return this;
        }

        public Builder withBody(Source<ByteString, ?> val) {
            body = val;
            return this;
        }

        public Builder withHeaders(Map<String, List<String>> val) {
            headers = val;
            return this;
        }

        public Builder withQueryParams(Map<String, List<String>> val) {
            queryParams = val;
            return this;
        }

        public Builder withRequestTimeout(Option<Duration> val) {
            requestTimeout = val;
            return this;
        }

        public Builder withFollowsRedirect(Option<Boolean> val) {
            followsRedirect = val;
            return this;
        }

        public Builder withVirtualHost(Option<String> val) {
            virtualHost = val;
            return this;
        }

        public Builder withContentType(ContentType val) {
            contentType = val;
            return this;
        }

        public Builder withMaterializer(ActorMaterializer val) {
            materializer = val;
            return this;
        }

        public WSRequest build() {
            return new WSRequest(this);
        }
    }
}
