package org.reactivecouchbase.sbessentials.libs.actions;

import akka.stream.javadsl.Source;
import akka.stream.javadsl.StreamConverters;
import akka.util.ByteString;
import javaslang.collection.HashMap;
import javaslang.collection.List;
import javaslang.collection.Map;
import org.reactivecouchbase.functional.Option;
import org.reactivecouchbase.json.JsValue;
import org.springframework.web.context.WebApplicationContext;
import org.w3c.dom.Node;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

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

    public Source<ByteString, ?> bodyAsStream() {
        return StreamConverters.fromInputStream(() -> getRequest().getInputStream());
    }

    public Option<String> header() {
        throw new RuntimeException("Not implemented yet");
    }

    public ByteString bodyAsBytes() {
        throw new RuntimeException("Not implemented yet");
    }

    public JsValue bodyAsJson() {
        throw new RuntimeException("Not implemented yet");
    }

    public Node bodyAsXml() {
        throw new RuntimeException("Not implemented yet");
    }

    public Map<String, List<String>> bodyAsURLForm() {
        throw new RuntimeException("Not implemented yet");
    }
}
