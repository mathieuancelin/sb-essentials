package org.reactivecouchbase.sbessentials.libs.ws;

import akka.actor.ActorSystem;
import akka.http.javadsl.model.HttpHeader;
import akka.http.javadsl.model.HttpResponse;
import akka.stream.ActorMaterializer;
import akka.stream.javadsl.Source;
import akka.util.ByteString;
import javaslang.collection.HashMap;
import javaslang.collection.List;
import javaslang.collection.Map;
import org.reactivecouchbase.concurrent.Future;
import org.reactivecouchbase.functional.Option;

public class WSResponse {

    public final HttpResponse underlying;

    private final Map<String, List<String>> headers;

    public WSResponse(HttpResponse underlying) {
        this.underlying = underlying;
        Map<String, List<String>> _headers = HashMap.empty();
        for (HttpHeader header : underlying.getHeaders()) {
            // System.out.println("received header : " + header.toString());
            if (!_headers.containsKey(header.name())) {
                _headers = _headers.put(header.name(), List.empty());
            }
            _headers = _headers.put(header.name(), _headers.get(header.name()).get().append(header.value()));
        }
        this.headers = _headers;
    }

    public Map<String, List<String>> headers() {
        return headers;
    }

    public int status() {
        return underlying.status().intValue();
    }

    public String statusText() {
        return underlying.status().defaultMessage();
    }

    public Option<String> header(String name) {
        return Option.fromJdkOptional(underlying.getHeader(name)).map(HttpHeader::value);
    }

    public List<WSCookie> cookies() {
        throw new RuntimeException("Not implemented yet");
    }

    public Option<WSCookie> cookie(String name) {
        throw new RuntimeException("Not implemented yet");
    }

    public Future<WSBody> body() {
        ActorMaterializer materializer = ActorMaterializer.create(WS.webApplicationContext.getBean(ActorSystem.class));
        Source<ByteString, ?> source = underlying.entity().getDataBytes();
        return Future.fromJdkCompletableFuture(
                source.runFold(ByteString.empty(), ByteString::concat, materializer).toCompletableFuture()
        ).map(WSBody::new);
    }

    public Source<ByteString, ?> bodyAsStream() {
        return underlying.entity().getDataBytes();
    }
}
