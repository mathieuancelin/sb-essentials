package org.reactivecouchbase.sbessentials.libs.actions;

import akka.actor.ActorSystem;
import akka.stream.ActorMaterializer;
import akka.stream.javadsl.Source;
import akka.stream.javadsl.StreamConverters;
import akka.util.ByteString;
import javaslang.collection.HashMap;
import javaslang.collection.List;
import javaslang.collection.Map;
import org.reactivecouchbase.concurrent.Future;
import org.reactivecouchbase.functional.Option;
import org.springframework.web.context.WebApplicationContext;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.ArrayList;
import java.util.Collections;

public class RequestContext {

    private final HashMap<String, Object> state;

    private final WebApplicationContext applicationContext;

    private final HttpServletRequest request;

    private final HttpServletResponse response;

    private final Map<String, List<String>> headers;

    public RequestContext(HashMap<String, Object> state, WebApplicationContext applicationContext, HttpServletRequest request, HttpServletResponse response) {
        this.state = state;
        this.applicationContext = applicationContext;
        this.request = request;
        this.response = response;
        Map<String, List<String>> _headers = HashMap.empty();
        ArrayList<String> headerNames = Collections.list(request.getHeaderNames());
        for (String name : headerNames) {
            _headers = _headers.put(name, List.ofAll(Collections.list(request.getHeaders(name))));
        }
        this.headers = _headers;
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

    public Future<RequestBody> body() {
        ActorMaterializer materializer = ActorMaterializer.create(Actions.webApplicationContext.getBean(ActorSystem.class));
        return Future.fromJdkCompletableFuture(
            bodyAsStream().runFold(ByteString.empty(), ByteString::concat, materializer).toCompletableFuture()
        ).map(RequestBody::new);
    }

    public Source<ByteString, ?> bodyAsStream() {
        return StreamConverters.fromInputStream(() -> getRequest().getInputStream());
    }

    public Option<String> header(String name) {
        return Option.apply(request.getHeader(name));
    }

    public Map<String, List<String>> headers() {
        return headers;
    }
}
