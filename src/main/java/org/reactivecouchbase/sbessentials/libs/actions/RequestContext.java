package org.reactivecouchbase.sbessentials.libs.actions;

import akka.actor.ActorSystem;
import akka.stream.ActorMaterializer;
import akka.stream.javadsl.Source;
import akka.stream.javadsl.StreamConverters;
import akka.util.ByteString;
import javaslang.collection.HashMap;
import javaslang.collection.List;
import javaslang.collection.Map;
import org.reactivecouchbase.common.Throwables;
import org.reactivecouchbase.concurrent.Await;
import org.reactivecouchbase.concurrent.Future;
import org.reactivecouchbase.functional.Option;
import org.reactivecouchbase.json.JsValue;
import org.reactivecouchbase.json.Json;
import org.springframework.web.context.WebApplicationContext;
import org.w3c.dom.Node;
import org.xml.sax.InputSource;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.StringReader;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicReference;

public class RequestContext {

    private final HashMap<String, Object> state;

    private final WebApplicationContext applicationContext;

    private final HttpServletRequest request;

    private final HttpServletResponse response;

    public RequestContext(HashMap<String, Object> state, WebApplicationContext applicationContext, HttpServletRequest request, HttpServletResponse response) {
        this.state = state;
        this.applicationContext = applicationContext;
        this.request = request;
        this.response = response;
    }

    public <T> T getBean(Class<T> clazz) {
        return applicationContext.getBean(clazz);
    }

    public <T> T getBean(Class<T> clazz, String name){
        return applicationContext.getBean(clazz, name);
    }

    public Object getValue(String key) {
        return this.state.get(key);
    }

    public <T> T getValue(String key, Class<T> clazz) {
        return this.state.get(key).map(clazz::cast).get();
    }

    public RequestContext setValue(String key, Object value) {
        if(key == null || value == null) {
            return this;
        } else {
            return new RequestContext(state.put(key, value), applicationContext, request, response);
        }
    }

    public HttpServletRequest getRequest() {
        return request;
    }

    public HttpServletResponse getResponse() {
        return response;
    }

    private final AtomicReference<ByteString> _bodyAsBytes = new AtomicReference<>(null);

    public Source<ByteString, ?> bodyAsStream() {
        return StreamConverters.fromInputStream(() -> getRequest().getInputStream());
    }

    public Option<String> header(String name) {
        return Option.apply(request.getHeader(name));
    }

    public Map<String, List<String>> headers() {
        throw new RuntimeException("Not implemented yet");
    }

    public ByteString bodyAsBytes() {
        if (_bodyAsBytes.get() == null) {
            ActorMaterializer materializer = ActorMaterializer.create(Actions.webApplicationContext.getBean(ActorSystem.class));
            Future<ByteString> fource = Future.fromJdkCompletableFuture(
                bodyAsStream().runFold(ByteString.empty(), ByteString::concat, materializer).toCompletableFuture()
            );
            ByteString bodyAsString = Await.resultForever(fource);
            _bodyAsBytes.compareAndSet(null, bodyAsString);
        }
        return _bodyAsBytes.get();
    }

    public String bodyAsString() {
        return bodyAsBytes().utf8String();
    }

    public JsValue bodyAsJson() {
        return Json.parse(bodyAsString());
    }

    public Node bodyAsXml() {
        try {
            return DocumentBuilderFactory.newInstance().newDocumentBuilder()
                    .parse(new InputSource(new StringReader(bodyAsString())));
        } catch (Exception e) {
            throw Throwables.propagate(e);
        }
    }

    public Map<String, List<String>> bodyAsURLForm() {
        Map<String, List<String>> form = HashMap.empty();
        String body = bodyAsString();
        List<String> parts = List.ofAll(Arrays.asList(body.split("&")));
        for (String part : parts) {
            String key = part.split("=")[0];
            String value = part.split("=")[1];
            if (!form.containsKey(key)) {
                form = form.put(key, List.empty());
            }
            form = form.put(key, form.get(key).get().append(value));
        }
        return form;
    }
}
