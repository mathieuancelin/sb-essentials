package org.reactivecouchbase.sbessentials.libs.ws;

import akka.http.javadsl.model.HttpRequest;
import akka.stream.javadsl.Source;
import akka.util.ByteString;
import javaslang.collection.List;
import javaslang.collection.Map;
import org.reactivecouchbase.common.Duration;
import org.reactivecouchbase.concurrent.Future;
import org.reactivecouchbase.functional.Option;
import org.reactivecouchbase.functional.Tuple;

// not used for now on
class WSRequest {

    public final String url;
    public final String method;
    public final Source<ByteString, ?> body;
    public final Map<String, List<String>> headers;
    public final Map<String, List<String>> queryString;
    public final Option<Duration> requestTimeout;
    public final Option<Boolean> followsRedirect;
    public final Option<String> virtualHost;

    private WSRequest(Builder builder) {
        url = builder.url;
        method = builder.method;
        body = builder.body;
        headers = builder.headers;
        queryString = builder.queryString;
        requestTimeout = builder.requestTimeout;
        followsRedirect = builder.followsRedirect;
        virtualHost = builder.virtualHost;
    }

    private static Tuple<String, String> split(String url) {
        String[] parts = url.split("\\/");
        String uri = url.split("\\/\\/")[1].split("\\/")[1];
        String host = parts[0] + "//" + parts[2];
        return new Tuple<>(host, uri);
    }

    public Future<WSResponse> call() {
        Tuple<String, String> hostAndUri = split(url);
        String uri = hostAndUri._1;
        String host = hostAndUri._2;
        HttpRequest httpRequest = HttpRequest.create(uri);

        return WS.call(host, httpRequest);
    }

    public static Builder newBuilder() {
        return new Builder();
    }

    public static Builder copy(WSRequest copy) {
        Builder builder = new Builder();
        builder.url = copy.url;
        builder.method = copy.method;
        builder.body = copy.body;
        builder.headers = copy.headers;
        builder.queryString = copy.queryString;
        builder.requestTimeout = copy.requestTimeout;
        builder.followsRedirect = copy.followsRedirect;
        builder.virtualHost = copy.virtualHost;
        return builder;
    }


    public static final class Builder {
        private String url;
        private String method;
        private Source<ByteString, ?> body;
        private Map<String, List<String>> headers;
        private Map<String, List<String>> queryString;
        private Option<Duration> requestTimeout;
        private Option<Boolean> followsRedirect;
        private Option<String> virtualHost;

        private Builder() {
        }

        public Builder withUrl(String val) {
            url = val;
            return this;
        }

        public Builder withMethod(String val) {
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

        public Builder withQueryString(Map<String, List<String>> val) {
            queryString = val;
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

        public WSRequest build() {
            return new WSRequest(this);
        }
    }
}
