package org.reactivecouchbase.sbessentials.libs.actions;

import javaslang.collection.HashMap;
import org.springframework.web.context.WebApplicationContext;

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
}
