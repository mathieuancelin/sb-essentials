package org.reactivecouchbase.sbessentials.libs.actions;

import javaslang.collection.*;
import javaslang.control.Option;

import javax.servlet.http.HttpServletRequest;

public class RequestQueryParams {

    private final Map<String, List<String>> queryParams;

    RequestQueryParams(HttpServletRequest request) {
        this.queryParams = Option.of(request.getQueryString()).map(s -> s.replace("?", "")).map(s -> List.of(s.split("\\&"))).map(params -> {
            Map<String, List<String>> queryParams = HashMap.empty();
            for (String param : params) {
                String key = param.split("\\=")[0];
                String value = param.split("\\=")[1];
                if (queryParams.containsKey(key)) {
                    queryParams = queryParams.put(key, queryParams.get(key).get().append(value));
                } else {
                    queryParams = queryParams.put(key, List.of(value));
                }
            }
            return queryParams;
        }).getOrElse(HashMap.empty());
    }

    public Map<String, List<String>> raw() {
        return queryParams;
    }

    public Map<String, String> simpleParams() {
        return queryParams.bimap(k -> k, Traversable::head);
    }

    public Set<String> paramsNames() {
        return queryParams.keySet();
    }

    public List<String> params(String name) {
        return queryParams.get(name).getOrElse(List.empty());
    }

    public Option<String> param(String name) {
        return queryParams.get(name).flatMap(Traversable::headOption);
    }
}
