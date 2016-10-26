package org.reactivecouchbase.sbessentials.libs.ws;

import akka.actor.ActorSystem;
import akka.http.javadsl.model.HttpHeader;
import akka.http.javadsl.model.HttpResponse;
import akka.stream.ActorMaterializer;
import akka.stream.javadsl.Source;
import akka.util.ByteString;
import javaslang.collection.List;
import javaslang.collection.Map;
import org.reactivecouchbase.common.Throwables;
import org.reactivecouchbase.concurrent.Await;
import org.reactivecouchbase.concurrent.Future;
import org.reactivecouchbase.functional.Option;
import org.reactivecouchbase.json.JsValue;
import org.reactivecouchbase.json.Json;
import org.w3c.dom.Node;
import org.xml.sax.InputSource;

import javax.xml.parsers.DocumentBuilderFactory;
import java.io.StringReader;
import java.util.concurrent.atomic.AtomicReference;

public class WSResponse {

    public final HttpResponse underlying;

    private final AtomicReference<ByteString> _bodyAsBytes = new AtomicReference<>(null);

    public WSResponse(HttpResponse underlying) {
        this.underlying = underlying;
    }

    public Map<String, List<String>> headers() {
        throw new RuntimeException("Not implemented yet");
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

    public String body() {
        return bodyAsBytes().utf8String();
    }

    public JsValue json() {
        return Json.parse(body());
    }

    public Node xml() {
        try {
            return DocumentBuilderFactory.newInstance().newDocumentBuilder()
                    .parse(new InputSource(new StringReader(body())));
        } catch (Exception e) {
            throw Throwables.propagate(e);
        }
    }

    public ByteString bodyAsBytes() {
        if (_bodyAsBytes.get() == null) {
            ActorMaterializer materializer = ActorMaterializer.create(WS.webApplicationContext.getBean(ActorSystem.class));
            Source<ByteString, ?> source = underlying.entity().getDataBytes();
            Future<ByteString> fource = Future.fromJdkCompletableFuture(
                    source.runFold(ByteString.empty(), ByteString::concat, materializer).toCompletableFuture()
            );
            ByteString bodyAsString = Await.resultForever(fource);
            _bodyAsBytes.compareAndSet(null, bodyAsString);
        }
        return _bodyAsBytes.get();
    }

    public Source<ByteString, ?> bodyAsStream() {
        return underlying.entity().getDataBytes();
    }
}
