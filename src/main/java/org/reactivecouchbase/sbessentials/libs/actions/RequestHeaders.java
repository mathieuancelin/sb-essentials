package org.reactivecouchbase.sbessentials.libs.actions;

import javaslang.collection.*;
import javaslang.control.Option;

import javax.servlet.http.HttpServletRequest;
import java.util.Collections;

public class RequestHeaders {

    private final Map<String, List<String>> headers;

    RequestHeaders(HttpServletRequest request) {
        this.headers = Option.of(request.getHeaderNames()).map(Collections::list).map(names -> {
            Map<String, List<String>> _headers = HashMap.empty();
            for (String name : names) {
                _headers = _headers.put(name, List.ofAll(Collections.list(request.getHeaders(name))));
            }
            return _headers;
        }).getOrElse(HashMap.empty());
    }

    public Option<String> header(String name) {
        return headers.get(name).flatMap(Traversable::headOption);
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
