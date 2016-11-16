package org.reactivecouchbase.sbessentials.libs.actions;

import javaslang.collection.HashMap;
import javaslang.collection.Map;
import javaslang.collection.Set;
import javaslang.control.Option;
import org.springframework.web.servlet.HandlerMapping;

import javax.servlet.http.HttpServletRequest;

public class RequestPathParams {

    private final Map<String, String> pathParams;

    RequestPathParams(HttpServletRequest request) {
        this.pathParams = Option.of(request.getAttribute(HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE))
                .map(o -> (java.util.Map<String, String>) o)
                .map(HashMap::ofAll).getOrElse(HashMap.empty());
    }

    public Map<String, String> raw() {
        return pathParams;
    }

    public Set<String> paramNames() {
        return pathParams.keySet();
    }

    public Option<String> param(String name) {
        return pathParams.get(name);
    }
}