package org.reactivecouchbase.sbessentials.libs.ws;

import akka.http.javadsl.model.HttpHeader;
import akka.http.javadsl.model.HttpResponse;
import akka.stream.ActorMaterializer;
import akka.stream.javadsl.AsPublisher;
import akka.stream.javadsl.Sink;
import akka.stream.javadsl.Source;
import akka.util.ByteString;
import javaslang.collection.HashMap;
import javaslang.collection.List;
import javaslang.collection.Map;
import javaslang.concurrent.Future;
import javaslang.control.Option;
import org.reactivestreams.Publisher;
import org.reactivecouchbase.sbessentials.config.Tools;

import java.util.concurrent.ExecutorService;

public class WSResponse {

    private final HttpResponse underlying;
    private final Map<String, List<String>> headers;

    WSResponse(HttpResponse underlying) {
        this.underlying = underlying;
        Map<String, List<String>> _headers = HashMap.empty();
        for (HttpHeader header : underlying.getHeaders()) {
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
        return Option.ofOptional(underlying.getHeader(name)).map(HttpHeader::value);
    }

    public Future<WSBody> body() {
        return body(InternalWSHelper.executor());
    }

    public Future<WSBody> body(ExecutorService ec) {
        ActorMaterializer materializer = InternalWSHelper.materializer();
        Source<ByteString, ?> source = underlying.entity().getDataBytes();
        return Tools.fromJdkCompletableFuture(ec,
                source.runFold(ByteString.empty(), ByteString::concat, materializer).toCompletableFuture()
        ).map(WSBody::new);
    }

    public Source<ByteString, ?> bodyAsStream() {
        return underlying.entity().getDataBytes();
    }

    public Publisher<ByteString> bodyAsPublisher(AsPublisher asPublisher) {
        ActorMaterializer materializer = InternalWSHelper.materializer();
        return underlying.entity().getDataBytes().runWith(Sink.asPublisher(asPublisher), materializer);
    }
}
