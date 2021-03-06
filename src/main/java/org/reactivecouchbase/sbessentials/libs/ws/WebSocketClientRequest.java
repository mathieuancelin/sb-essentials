package org.reactivecouchbase.sbessentials.libs.ws;

import akka.actor.ActorSystem;
import akka.http.javadsl.Http;
import akka.http.javadsl.model.HttpHeader;
import akka.http.javadsl.model.headers.RawHeader;
import akka.http.javadsl.model.ws.Message;
import akka.http.javadsl.model.ws.WebSocketRequest;
import akka.http.javadsl.model.ws.WebSocketUpgradeResponse;
import akka.japi.Pair;
import akka.stream.ActorMaterializer;
import akka.stream.javadsl.Flow;
import javaslang.collection.HashMap;
import javaslang.collection.List;
import javaslang.collection.Map;
import org.reactivecouchbase.concurrent.Future;
import org.reactivestreams.Processor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CompletionStage;

public class WebSocketClientRequest {

    static final Logger logger = LoggerFactory.getLogger(WebSocketClientRequest.class);

    private final ActorSystem system;
    private final ActorMaterializer materializer;
    private final String host;
    private final Http http;
    private final String path;
    private final Map<String, List<String>> headers;
    private final Map<String, List<String>> queryParams;

    public WebSocketClientRequest(ActorSystem system, ActorMaterializer materializer, Http http, String host, String path) {
        this.system = system;
        this.materializer = materializer;
        this.host = host;
        this.http = http;
        this.path = path;
        this.headers = HashMap.empty();
        this.queryParams = HashMap.empty();
    }

    private WebSocketClientRequest(Builder builder) {
        system = builder.system;
        materializer = builder.materializer;
        host = builder.host;
        http = builder.http;
        path = builder.path;
        headers = builder.headers;
        queryParams = builder.queryParams;
    }

    static Builder newBuilder() {
        return new Builder();
    }

    public static Builder newBuilder(WebSocketClientRequest copy) {
        Builder builder = new Builder();
        builder.system = copy.system;
        builder.materializer = copy.materializer;
        builder.host = copy.host;
        builder.http = copy.http;
        builder.path = copy.path;
        builder.headers = copy.headers;
        builder.queryParams = copy.queryParams;
        return builder;
    }

    Builder copy() {
        return newBuilder(this);
    }

    public WebSocketClientRequest withPath(String val) {
        return copy().withPath(val).build();
    }

    public WebSocketClientRequest withHeaders(Map<String, List<String>> val) {
        return copy().withHeaders(val).build();
    }

    public WebSocketClientRequest withHeader(String name, String value) {
        Map<String, List<String>> _headers = headers;
        if (!_headers.containsKey(name)) {
            _headers = _headers.put(name, List.of(value));
        } else {
            _headers = _headers.put(name, _headers.get(name).get().append(value));
        }
        return copy().withHeaders(_headers).build();
    }

    public WebSocketClientRequest withQueryParams(Map<String, List<String>> val) {
        return copy().withQueryParams(val).build();
    }

    public WebSocketClientRequest withQueryParam(String name, Object value) {
        Map<String, List<String>> _queryString = queryParams;
        if (!_queryString.containsKey(name)) {
            _queryString = _queryString.put(name, List.of(value.toString()));
        } else {
            _queryString = _queryString.put(name, _queryString.get(name).get().append(value.toString()));
        }
        return copy().withQueryParams(_queryString).build();
    }

    public WebSocketClientRequest addPathSegment(String path) {
        return copy().withPath(this.path + "/" + path).build();
    }

    public WebSocketClientRequest addPathSegment(Object path) {
        return addPathSegment(path.toString());
    }

    public Future<WebSocketUpgradeResponse> callNoMat(Processor<Message, Message> flow) {
        return callNoMat(Flow.fromProcessor(() -> flow));
    }

    public <T> WebSocketConnections<T> call(Processor<Message, Message> flow, T materialized) {
        return call(Flow.fromProcessorMat(() -> Pair.apply(flow, materialized)));
    }

    public <T> WebSocketConnections<T> call(Flow<Message, Message, T> flow) {
        String _queryString = queryParams.toList().flatMap(tuple -> tuple._2.map(v -> tuple._1 + "=" + v)).mkString("&");
        List<HttpHeader> _headers = headers.toList().flatMap(tuple -> tuple._2.map(v -> RawHeader.create(tuple._1, v)));
        String url = host + path.replace("//", "/") + (queryParams.isEmpty() ? "" : "?" + _queryString);
        WebSocketRequest request = WebSocketRequest.create(url);
        request = _headers.foldLeft(request, WebSocketRequest::addHeader);
        final Pair<CompletionStage<WebSocketUpgradeResponse>, T> pair =
            Http.get(system).singleWebSocketRequest(
                request,
                flow,
                materializer
            );
        final CompletionStage<WebSocketUpgradeResponse> connected = pair.first();
        final T closed = pair.second();
        return new WebSocketConnections<T>(connected, closed);
    }

    public Future<WebSocketUpgradeResponse> callNoMat(Flow<Message, Message, ?> flow) {
        String _queryString = queryParams.toList().flatMap(tuple -> tuple._2.map(v -> tuple._1 + "=" + v)).mkString("&");
        List<HttpHeader> _headers = headers.toList().flatMap(tuple -> tuple._2.map(v -> RawHeader.create(tuple._1, v)));
        String url = host + path.replace("//", "/") + (queryParams.isEmpty() ? "" : "?" + _queryString);
        WebSocketRequest request = WebSocketRequest.create(url);
        request = _headers.foldLeft(request, WebSocketRequest::addHeader);
        final Pair<CompletionStage<WebSocketUpgradeResponse>, ?> pair =
                Http.get(system).singleWebSocketRequest(
                        request,
                        flow,
                        materializer
                );
        return Future.from(pair.first());
    }

    public static class WebSocketConnections<T> {

        private final Future<WebSocketUpgradeResponse> response;

        private final T materialized;

        WebSocketConnections(CompletionStage<WebSocketUpgradeResponse> response, T materialized) {
            this.response = Future.from(response);
            this.materialized = materialized;
        }

        public Future<WebSocketUpgradeResponse> response() {
            return response;
        }

        public T materialized() {
            return materialized;
        }
    }


    static final class Builder {
        private ActorSystem system;
        private ActorMaterializer materializer;
        private String host;
        private Http http;
        private String path;
        private Map<String, List<String>> headers;
        private Map<String, List<String>> queryParams;

        private Builder() {
        }

        public Builder withSystem(ActorSystem val) {
            system = val;
            return this;
        }

        public Builder withMaterializer(ActorMaterializer val) {
            materializer = val;
            return this;
        }

        public Builder withHost(String val) {
            host = val;
            return this;
        }

        public Builder withHttp(Http val) {
            http = val;
            return this;
        }

        public Builder withPath(String val) {
            path = val;
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

        public WebSocketClientRequest build() {
            return new WebSocketClientRequest(this);
        }
    }
}
