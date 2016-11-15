package org.reactivecouchbase.sbessentials.libs.ws;

import akka.Done;
import akka.actor.ActorSystem;
import akka.http.javadsl.Http;
import akka.http.javadsl.model.HttpHeader;
import akka.http.javadsl.model.StatusCodes;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CompletionStage;
import java.util.function.Function;

public class WebSocketClientRequest {

    static final Logger logger = LoggerFactory.getLogger(WebSocketClientRequest.class);

    private final ActorSystem system;
    private final ActorMaterializer materializer;
    private final String host;
    private final Http http;
    private final String path;
    private final Map<String, List<String>> headers;
    private final Map<String, List<String>> queryParams;
    private final Function<WebSocketUpgradeResponse, Done> upgradeHandler;

    public WebSocketClientRequest(ActorSystem system, ActorMaterializer materializer, Http http, String host, String path) {
        this.system = system;
        this.materializer = materializer;
        this.host = host;
        this.http = http;
        this.path = path;
        this.headers = HashMap.empty();
        this.queryParams = HashMap.empty();
        this.upgradeHandler = upgrade -> {
            logger.trace("Upgrade here " + upgrade.response().status());
            if (upgrade.response().status().equals(StatusCodes.SWITCHING_PROTOCOLS)) {
                return Done.getInstance();
            } else {
                throw new RuntimeException("Connection failed: " + upgrade.response().status());
            }
        };
    }

    private WebSocketClientRequest(Builder builder) {
        system = builder.system;
        materializer = builder.materializer;
        host = builder.host;
        http = builder.http;
        path = builder.path;
        headers = builder.headers;
        queryParams = builder.queryParams;
        upgradeHandler = builder.upgradeHandler;
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
        builder.upgradeHandler = copy.upgradeHandler;
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

    public WebSocketClientRequest withQueryParam(String name, String value) {
        Map<String, List<String>> _queryString = queryParams;
        if (!_queryString.containsKey(name)) {
            _queryString = _queryString.put(name, List.of(value));
        } else {
            _queryString = _queryString.put(name, _queryString.get(name).get().append(value));
        }
        return copy().withQueryParams(_queryString).build();
    }

    public WebSocketClientRequest addPathSegment(String path) {
        return copy().withPath(this.path + "/" + path).build();
    }

    public WebSocketClientRequest onRequestUpgrade(Function<WebSocketUpgradeResponse, Done> handler) {
        return copy().withUpgradeHandler(handler).build();
    }

    public WebSocketConnections call(Flow<Message, Message, CompletionStage<Done>> flow) {
        String _queryString = queryParams.toList().flatMap(tuple -> tuple._2.map(v -> tuple._1 + "=" + v)).mkString("&");
        List<HttpHeader> _headers = headers.toList().flatMap(tuple -> tuple._2.map(v -> RawHeader.create(tuple._1, v)));
        String url = host + path + (queryParams.isEmpty() ? "" : "?" + _queryString);
        WebSocketRequest request = WebSocketRequest.create(url);
        request = _headers.foldLeft(request, WebSocketRequest::addHeader);
        final Pair<CompletionStage<WebSocketUpgradeResponse>, CompletionStage<Done>> pair =
            Http.get(system).singleWebSocketRequest(
                request,
                flow,
                materializer
            );
        final CompletionStage<Done> connected = pair.first().thenApply(this.upgradeHandler);
        final CompletionStage<Done> closed = pair.second();
        return new WebSocketConnections(
            Future.from(connected),
            Future.from(closed)
        );
    }

    public static class WebSocketConnections {
        private final Future<Done> connectionOpened;
        private final Future<Done> connectionClosed;
        WebSocketConnections(Future<Done> connectionOpened, Future<Done> connectionClosed) {
            this.connectionOpened = connectionOpened;
            this.connectionClosed = connectionClosed;
        }

        public Future<Done> connectionOpened() {
            return connectionOpened;
        }

        public Future<Done> connectionClosed() {
            return connectionClosed;
        }

        public void closeConnection() {
            // TODO : with completablefuture stuff
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
        private Function<WebSocketUpgradeResponse, Done> upgradeHandler;

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

        public Builder withUpgradeHandler(Function<WebSocketUpgradeResponse, Done> val) {
            upgradeHandler = val;
            return this;
        }

        public WebSocketClientRequest build() {
            return new WebSocketClientRequest(this);
        }
    }
}
