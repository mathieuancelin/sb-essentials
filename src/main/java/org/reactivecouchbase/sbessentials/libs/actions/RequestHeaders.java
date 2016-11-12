package org.reactivecouchbase.sbessentials.libs.actions;

import javaslang.collection.*;
import org.reactivecouchbase.functional.Option;

import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.Collections;

public class RequestHeaders {

    private final Map<String, List<String>> headers;
    private final HttpServletRequest request;

    RequestHeaders(HttpServletRequest request) {
        this.request = request;
        Map<String, List<String>> _headers = HashMap.empty();
        ArrayList<String> headerNames = Collections.list(request.getHeaderNames());
        for (String name : headerNames) {
            _headers = _headers.put(name, List.ofAll(Collections.list(request.getHeaders(name))));
        }
        this.headers = _headers;
    }

    public Option<String> header(String name) {
        return Option.apply(request.getHeader(name));
    }

    public Map<String, List<String>> headers() {
        return headers;
    }

    public Map<String, String> simpleHeaders() {
        return headers.bimap(k -> k, Traversable::head);
    }

    public Set<String> headerNames() {
        return headers.keySet();
    }

    public Map<String, List<String>> raw() {
        return headers;
    }
}
