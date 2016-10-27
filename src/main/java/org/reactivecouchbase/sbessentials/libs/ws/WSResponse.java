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
import org.reactivecouchbase.concurrent.Await;
import org.reactivecouchbase.concurrent.Future;
import org.reactivecouchbase.functional.Option;
import java.util.concurrent.atomic.AtomicReference;

public class WSResponse {

    public final HttpResponse underlying;

    private final AtomicReference<ByteString> _bodyAsBytes = new AtomicReference<>(null);
    private final Map<String, List<String>> headers;

    public WSResponse(HttpResponse underlying) {
        this.underlying = underlying;
        Map<String, List<String>> headers = HashMap.empty();
        for (HttpHeader header : underlying.getHeaders()) {
            if (headers.containsKey(header.lowercaseName())) {
                headers = headers.put(header.lowercaseName(), List.empty());
            }
            headers = headers.put(header.lowercaseName(), headers.get(header.lowercaseName()).get().append(header.value()));
        }
        this.headers = headers;
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
